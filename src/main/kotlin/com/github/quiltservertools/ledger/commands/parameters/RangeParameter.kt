package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

private const val MAX_SIZE = 9

class RangeParameter : SimpleParameter<Int>() {
    override fun parse(stringReader: StringReader): Int = IntegerArgumentType.integer(1).parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        for (i in 1..MAX_SIZE) builder.suggest(remaining + i)
        return builder.buildFuture()
    }
}
