package com.github.quiltservertools.ledger.commands.parameters

import com.github.quiltservertools.ledger.database.DatabaseManager
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource

class SourceParameter : SimpleParameter<String>() {
    override fun parse(stringReader: StringReader): String {
        val i: Int = stringReader.cursor

        while (stringReader.canRead() && stringReader.peek() != ' ') {
            stringReader.skip()
        }

        return stringReader.string.substring(i, stringReader.cursor)
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val stringReader = StringReader(builder.input)
        stringReader.cursor = builder.start

        val players = context.source.playerNames
        DatabaseManager.getKnownSources().forEach {
            players.add("@$it")
        }
        // TODO suggest non-player sources

        return CommandSource.suggestMatching(
            players,
            builder
        )
    }
}
