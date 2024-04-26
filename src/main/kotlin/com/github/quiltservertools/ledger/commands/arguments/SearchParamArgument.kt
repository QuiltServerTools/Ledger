package com.github.quiltservertools.ledger.commands.arguments

import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.parameters.ActionParameter
import com.github.quiltservertools.ledger.commands.parameters.DimensionParameter
import com.github.quiltservertools.ledger.commands.parameters.ObjectParameter
import com.github.quiltservertools.ledger.commands.parameters.RangeParameter
import com.github.quiltservertools.ledger.commands.parameters.RollbackStatusParameter
import com.github.quiltservertools.ledger.commands.parameters.SimpleParameter
import com.github.quiltservertools.ledger.commands.parameters.SourceParameter
import com.github.quiltservertools.ledger.commands.parameters.TimeParameter
import com.github.quiltservertools.ledger.utility.Negatable
import com.google.common.collect.HashMultimap
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

object SearchParamArgument {
    private val paramSuggesters = HashMap<String, Parameter<*>>()

    init {
        paramSuggesters["action"] = NegatableParameter(ActionParameter())
        paramSuggesters["source"] = NegatableParameter(SourceParameter())
        paramSuggesters["range"] = Parameter(RangeParameter())
        paramSuggesters["object"] = NegatableParameter(ObjectParameter())
        paramSuggesters["world"] = NegatableParameter(DimensionParameter())
        paramSuggesters["before"] = Parameter(TimeParameter())
        paramSuggesters["after"] = Parameter(TimeParameter())
        paramSuggesters["rolledback"] = Parameter(RollbackStatusParameter())
    }

    fun argument(name: String): RequiredArgumentBuilder<ServerCommandSource, String> {
        return CommandManager.argument(name, StringArgumentType.greedyString())
            .suggests { context, builder ->
                val input = builder.input
                val lastSpaceIndex = input.lastIndexOf(' ')
                val inputArr = input.toCharArray()
                var lastColonIndex = -1
                for (i in inputArr.indices.reversed()) {
                    val c = inputArr[i]
                    if (c == ':') {
                        lastColonIndex = i
                    } else if (lastColonIndex != -1 && c == ' ') {
                        break
                    }
                }
                if (lastColonIndex == -1) {
                    val offsetBuilder = builder.createOffset(lastSpaceIndex + 1)
                    builder.add(suggestCriteria(offsetBuilder))
                } else {
                    val spaceSplit = input.substring(0, lastColonIndex).split(" ").toTypedArray()
                    val criterion = spaceSplit[spaceSplit.size - 1]
                    val criteriaArg = input.substring(lastColonIndex + 1)
                    return@suggests if (!paramSuggesters.containsKey(criterion)) {
                        builder.buildFuture()
                    } else {
                        val suggester = paramSuggesters[criterion]
                        val remaining = suggester!!.getRemaining(criteriaArg)
                        if (remaining > 0) {
                            val offsetBuilder = builder.createOffset(input.length - remaining + 1)
                            suggestCriteria(offsetBuilder).buildFuture()
                        } else {
                            val offsetBuilder = builder.createOffset(lastColonIndex + 1)
                            suggester.listSuggestions(context, offsetBuilder)
                        }
                    }
                }
                return@suggests builder.buildFuture()
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun get(input: String, source: ServerCommandSource): ActionSearchParams {
        val reader = StringReader(input)
        val result = HashMultimap.create<String, Any>()
        while (reader.canRead()) {
            val propertyName = reader.readStringUntil(':').trim(' ')
            val parameter = paramSuggesters[propertyName]
                ?: throw SimpleCommandExceptionType(LiteralMessage("Unknown property value: $propertyName"))
                    .create()
            val value =
                if (parameter is NegatableParameter) parameter.parseNegatable(reader) else parameter.parse(reader)
            result.put(propertyName, value)
        }

        val builder = ActionSearchParams.Builder()

        for (entry in result.entries()) {
            val param = entry.key
            val value = entry.value

            when (param) {
                "range" -> {
                    val range = value as Int - 1
                    builder.bounds = BlockBox.create(
                        BlockPos.ofFloored(source.position).subtract(Vec3i(range, range, range)),
                        BlockPos.ofFloored(source.position).add(Vec3i(range, range, range))
                    )
                    val world = Negatable.allow(source.world.registryKey.value)
                    if (builder.worlds == null) builder.worlds = mutableSetOf(world) else builder.worlds!!.add(world)
                }
                "world" -> {
                    val world = value as Negatable<Identifier>
                    if (builder.worlds == null) builder.worlds = mutableSetOf(world) else builder.worlds!!.add(world)
                }
                "object" -> {
                    val objectIds = (value as Negatable<List<Identifier>>).property.map {
                        if (value.allowed) Negatable.allow(it) else Negatable.deny(it)
                    }.toMutableSet()

                    if (builder.objects == null) {
                        builder.objects = objectIds
                    } else {
                        builder.objects!!.addAll(objectIds)
                    }
                }
                "source" -> {
                    val sourceInput = value as Negatable<String>
                    if (sourceInput.property.startsWith('@')) {
                        val nonPlayer = Negatable(sourceInput.property.trim('@'), sourceInput.allowed)
                        if (builder.sourceNames == null) {
                            builder.sourceNames =
                                mutableSetOf(nonPlayer)
                        } else {
                            builder.sourceNames!!.add(nonPlayer)
                        }
                    } else {
                        val profile = source.server.userCache?.findByName(sourceInput.property)
                        // If the player doesn't exist use a random UUID to make the query not match
                        val id = profile?.orElse(null)?.id ?: UUID.randomUUID()

                        if (id != null) {
                            val playerIdEntry = Negatable(id, sourceInput.allowed)
                            if (builder.sourcePlayerIds == null) {
                                builder.sourcePlayerIds =
                                    mutableSetOf(playerIdEntry)
                            } else {
                                builder.sourcePlayerIds!!.add(playerIdEntry)
                            }
                        }
                    }
                }
                "action" -> {
                    val action = value as Negatable<String>
                    if (builder.actions == null) {
                        builder.actions = mutableSetOf(action)
                    } else {
                        builder.actions!!.add(action)
                    }
                }
                "before" -> {
                    val time = value as Instant
                    builder.before = time
                }
                "after" -> {
                    val time = value as Instant
                    builder.after = time
                }
                "rolledback" -> {
                    val rolledBack = value as Boolean
                    builder.rolledBack = rolledBack
                }
            }
        }

        return builder.build()
    }

    fun get(context: CommandContext<ServerCommandSource>, name: String): ActionSearchParams {
        val input = StringArgumentType.getString(context, name)
        return get(input, context.source)
    }

    private fun suggestCriteria(builder: SuggestionsBuilder): SuggestionsBuilder {
        val input = builder.remaining.lowercase()
        for (param in paramSuggesters.keys) {
            if (param.startsWith(input)) {
                builder.suggest("$param:")
            }
        }
        return builder
    }

    private open class Parameter<T>(private val parameter: SimpleParameter<T>) {

        open fun listSuggestions(
            context: CommandContext<ServerCommandSource>,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            return try {
                parameter.getSuggestions(context, builder)
            } catch (e: CommandSyntaxException) {
                builder.buildFuture()
            }
        }

        open fun getRemaining(s: String): Int {
            val reader = StringReader(s)
            parameter.parse(reader)
            return reader.remainingLength
        }

        @Throws(CommandSyntaxException::class)
        open fun parse(reader: StringReader) = parameter.parse(reader)
    }

    private class NegatableParameter<T>(parameter: SimpleParameter<T>) : Parameter<T>(parameter) {
        override fun listSuggestions(
            context: CommandContext<ServerCommandSource>,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            val builder = if (builder.remaining.startsWith("!")) builder.createOffset(builder.start + 1) else builder
            return super.listSuggestions(context, builder)
        }

        override fun getRemaining(s: String): Int {
            val input = if (s.startsWith("!")) s.substring(1) else s
            return super.getRemaining(input)
        }

        @Throws(CommandSyntaxException::class)
        fun parseNegatable(reader: StringReader): Negatable<T> {
            if (reader.string.isEmpty()) return Negatable.allow(parse(reader))
            return if (reader.string[reader.cursor] == '!') {
                reader.skip()
                Negatable.deny(super.parse(reader))
            } else {
                Negatable.allow(super.parse(reader))
            }
        }
    }
}
