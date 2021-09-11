package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class DimensionParameter : SimpleParameter<Identifier>() {
    override fun parse(stringReader: StringReader): Identifier = DimensionArgumentType.dimension().parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = DimensionArgumentType.dimension().listSuggestions(context, builder)
}
