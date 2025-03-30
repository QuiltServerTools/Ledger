package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

private const val MAX_SIZE = 9
private const val GLOBAL = "@global"

class RangeParameter : SimpleParameter<Int?>() {
    override fun parse(stringReader: StringReader): Int? {
        val start: Int = stringReader.cursor

        while (stringReader.canRead() && stringReader.peek() != ' ') {
            stringReader.skip()
        }

        val argument = stringReader.string.substring(start, stringReader.cursor)
        if (argument == GLOBAL) {
            return null
        } else {
            stringReader.cursor = start
            return IntegerArgumentType.integer(1).parse(stringReader)
        }
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        val reader = StringReader(remaining)

        CommandSource.suggestMatching(listOf(GLOBAL), builder)
        var suggestNumber = false
        try {
            reader.readInt()
            suggestNumber = true
        } catch (_: CommandSyntaxException) {
            suggestNumber = remaining.isEmpty()
        }

        if (suggestNumber) {
            for (i in 1..MAX_SIZE) builder.suggest(remaining + i)
        }

        return builder.buildFuture()
    }
}
