package us.potatoboy.ledger.commands.arguments

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
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.parameters.*
import java.time.Duration
import java.util.concurrent.CompletableFuture

object SearchParamArgument {
    private val paramSuggesters = HashMap<String, Parameter>()

    init {
        paramSuggesters["action"] = Parameter(ActionParameter())
        paramSuggesters["source"] = Parameter(SourceParameter())
        paramSuggesters["range"] = Parameter(RangeParameter())
        paramSuggesters["object"] = Parameter(ObjectParameter())
        paramSuggesters["world"] = Parameter(DimensionParameter())
        paramSuggesters["time"] = Parameter(TimeParameter())
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

    fun get(input: String, source: ServerCommandSource): ActionSearchParams {
        val reader = StringReader(input)
        val result = HashMultimap.create<String, Any>()
        while (reader.canRead()) {
            val propertyName = reader.readStringUntil(':').trim(' ')
            val suggester = paramSuggesters[propertyName]
                ?: throw SimpleCommandExceptionType(LiteralMessage("Unknown property value: $propertyName"))
                    .create()
            result.put(propertyName, suggester.parse(reader))
        }

        val builder = ActionSearchParams.Builder()

        for (entry in result.entries()) {
            val param = entry.key
            val value = entry.value

            when (param) {
                "range" -> {
                    val range = value as Int - 1
                    builder.min =
                        BlockPos(source.position.subtract(range.toDouble(), range.toDouble(), range.toDouble()))
                    builder.max =
                        BlockPos(source.position.add(range.toDouble(), range.toDouble(), range.toDouble()))
                }
                "world" -> {
                    val world = value as Identifier
                    if (builder.worlds == null) builder.worlds = mutableSetOf(world) else builder.worlds!!.add(world)
                }
                "object" -> {
                    val objectId = value as Identifier
                    if (builder.objects == null) {
                        builder.objects = mutableSetOf(objectId)
                    } else {
                        builder.objects!!.add(objectId)
                    }
                }
                "source" -> {
                    var playerName = value as String
                    if (playerName.startsWith('@')) {
                        playerName = playerName.trim('@')
                        if (builder.sourceNames == null) {
                            builder.sourceNames =
                                mutableSetOf(playerName)
                        } else {
                            builder.sourceNames!!.add(playerName)
                        }
                    } else {
                        if (builder.sourcePlayerNames == null) {
                            builder.sourcePlayerNames =
                                mutableSetOf(playerName)
                        } else {
                            builder.sourcePlayerNames!!.add(playerName)
                        }
                    }
                }
                "action" -> {
                    val action = value as String
                    if (builder.actions == null) {
                        builder.actions = mutableSetOf(action)
                    } else {
                        builder.actions!!.add(action)
                    }
                }
                "time" -> {
                    val time = value as Duration
                    builder.time = time
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

    private class Parameter(private val parameter: SimpleParameter<*>) {

        fun listSuggestions(
            context: CommandContext<ServerCommandSource?>?,
            builder: SuggestionsBuilder
        ): CompletableFuture<Suggestions>? {
            return try {
                parameter.getSuggestions(context, builder)
            } catch (e: CommandSyntaxException) {
                builder.buildFuture()
            }
        }

        fun getRemaining(s: String): Int {
            val reader = StringReader(s)
            parameter.parse(reader)
            return reader.remainingLength
        }

        @Throws(CommandSyntaxException::class)
        fun parse(reader: StringReader) = parameter.parse(reader)!!
    }
}
