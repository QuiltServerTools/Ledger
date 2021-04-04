package us.potatoboy.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.LiteralText
import java.time.Duration
import java.util.concurrent.CompletableFuture

class TimeParameter : SimpleParameter<Duration>() {
    private val units = listOf('s', 'm', 'h', 'd', 'w')

    override fun parse(stringReader: StringReader): Duration {
        val i: Int = stringReader.cursor

        while (stringReader.canRead() && isCharValid(stringReader.peek())) {
            stringReader.skip()
        }

        val input = stringReader.string.substring(i, stringReader.cursor).toLowerCase()

        val timeRegex = Regex("([0-9]+)(s|m|h|d|w)")
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
                    else -> throw SimpleCommandExceptionType(LiteralText("")).create()
                }
            }
        }

        return duration
    }

    private fun isCharValid(c: Char): Boolean {
        return c in '0'..'9' || c in 'a'..'z'
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remaining.toLowerCase()
        if (remaining.isEmpty()) {
            for (i in 1..9) builder.suggest(i)
        } else {
            val end = remaining.last()
            if (units.contains(end)) {
                for (i in 1..10) builder.suggest(remaining + i)
                return builder.buildFuture()
            }

            for (unit in units) {
                builder.suggest(remaining + unit)
            }
        }

        return builder.buildFuture()
    }
}