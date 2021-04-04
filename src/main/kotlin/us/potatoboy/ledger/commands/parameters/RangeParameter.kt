package us.potatoboy.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

class RangeParameter: SimpleParameter<Int>() {
    override fun parse(stringReader: StringReader): Int {
        return IntegerArgumentType.integer(1).parse(stringReader)
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.toLowerCase()
        for (i in 1..9) builder.suggest(remaining + i)
        return builder.buildFuture()
    }
}