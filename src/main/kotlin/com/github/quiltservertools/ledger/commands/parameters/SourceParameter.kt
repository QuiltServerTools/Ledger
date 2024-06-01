package com.github.quiltservertools.ledger.commands.parameters

import com.github.quiltservertools.ledger.database.DatabaseManager
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

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

        val sources = context.source.playerNames
        DatabaseManager.getKnownSources().forEach {
            sources.add("@$it")
        }
        return CommandSource.suggestMatching(
            sources,
            builder
        )
    }
}
