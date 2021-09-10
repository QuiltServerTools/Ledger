package com.github.quiltservertools.ledger.commands.parameters

import com.github.quiltservertools.ledger.utility.Negatable
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class DimensionParameter : SimpleParameter<Identifier>() {
    override fun parse(stringReader: StringReader): Negatable<Identifier> =
        Negatable.getNegatable(stringReader, DimensionArgumentType.dimension())

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>?,
        builder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions> {
        if (builder!!.remaining.startsWith('!')) {
            context!!.source.server.worldRegistryKeys.forEach {
                builder.suggest("!${it.value}")
            }
            return builder.buildFuture()
        }
        return DimensionArgumentType.dimension().listSuggestions(context, builder)
    }
}
