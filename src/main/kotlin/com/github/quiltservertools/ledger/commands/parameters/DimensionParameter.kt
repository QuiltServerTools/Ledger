package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.resources.ResourceLocation
import java.util.concurrent.CompletableFuture

class DimensionParameter : SimpleParameter<ResourceLocation>() {
    override fun parse(stringReader: StringReader): ResourceLocation = DimensionArgument.dimension().parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = DimensionArgument.dimension().listSuggestions(context, builder)
}
