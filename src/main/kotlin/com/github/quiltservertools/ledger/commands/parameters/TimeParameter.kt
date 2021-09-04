package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

private const val MAX_SIZE = 9

class TimeParameter : SimpleParameter<Instant>() {
    private val units = listOf('s', 'm', 'h', 'd', 'w')

    @Suppress("MagicNumber")
    override fun parse(stringReader: StringReader): Instant {
        val i: Int = stringReader.cursor

        while (stringReader.canRead() && isCharValid(stringReader.peek())) {
            stringReader.skip()
        }

        val input = stringReader.string.substring(i, stringReader.cursor).lowercase()

        val timeRegex = Regex("([0-9]+)([smhdw])")
        val times = timeRegex.findAll(input)

        var duration = Duration.ZERO
        for (time in times) {
            if (time.groups.size == 3) {
                val timeValue = time.groupValues[1].toLong()
                val timeUnit = time.groupValues[2]

                when (timeUnit) {
                    "s" -> duration = duration.plusSeconds(timeValue)
                    "m" -> duration = duration.plusMinutes(timeValue)
                    "h" -> duration = duration.plusHours(timeValue)
                    "d" -> duration = duration.plusDays(timeValue)
                    "w" -> duration = duration.plusDays(timeValue * 7)
                }
            }
        }

        return Instant.now().minus(duration)
    }

    private fun isCharValid(c: Char) = c in '0'..'9' || c in 'a'..'z'

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.lowercase()
        for (unit in units) {
            if (remaining.isEmpty()) {
                for (i in 1..MAX_SIZE) builder.suggest(i.toString() + unit)
            } else {
                val end = remaining.last()
                if (end in '1'..'9') {
                    builder.suggest(remaining + unit)
                }
            }
        }
        return builder.buildFuture()
    }
}
