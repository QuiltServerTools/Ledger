package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.resources.Identifier
import java.util.concurrent.CompletableFuture

class DimensionParameter : SimpleParameter<Identifier>() {
    override fun parse(stringReader: StringReader): Identifier = DimensionArgument.dimension().parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = DimensionArgument.dimension().listSuggestions(context, builder)
}
