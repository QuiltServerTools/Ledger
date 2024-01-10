package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.Networking.hasNetworking
import com.github.quiltservertools.ledger.network.packet.action.ActionPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

object MessageUtils {
    @OptIn(ExperimentalTime::class)
    suspend fun sendSearchResults(source: ServerCommandSource, results: SearchResults, header: Text) {

        // If the player has a Ledger compatible client, we send results as action packets rather than as chat messages
        if (source.hasPlayer() && source.playerOrThrow.hasNetworking()) {
            for (n in results.page..results.pages) {
                val networkResults = DatabaseManager.searchActions(results.searchParams, n)
                networkResults.actions.forEach {
                    val packet = ActionPacket()
                    packet.populate(it)
                    ServerPlayNetworking.send(source.player, packet.channel, packet.buf)
                }
            }
            return
        }

        source.sendFeedback({ header }, false)

        results.actions.forEach { actionType ->
            source.sendFeedback({ actionType.getMessage() }, false)
        }

        source.sendFeedback(
            {
                Text.translatable(
                    "text.ledger.footer.search",
                    Text.translatable("text.ledger.footer.page_backward").setStyle(TextColorPallet.primaryVariant)
                        .styled {
                            if (results.page > 1) {
                                it.withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.translatable("text.ledger.footer.page_backward.hover")
                                    )
                                ).withClickEvent(
                                    ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lg pg ${results.page - 1}")
                                )
                            } else {
                                Style.EMPTY
                            }
                        },
                    results.page.toString().literal().setStyle(TextColorPallet.primaryVariant),
                    results.pages.toString().literal().setStyle(TextColorPallet.primaryVariant),
                    Text.translatable("text.ledger.footer.page_forward").setStyle(TextColorPallet.primaryVariant)
                        .styled {
                            if (results.page < results.pages) {
                                it.withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.translatable("text.ledger.footer.page_forward.hover")
                                    )
                                ).withClickEvent(
                                    ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lg pg ${results.page + 1}")
                                )
                            } else {
                                Style.EMPTY
                            }
                        }
                ).setStyle(TextColorPallet.primary)
            },
            false
        )
    }

    fun sendPlayerMessage(source: ServerCommandSource, results: List<PlayerResult>) {
        if (results.isEmpty()) {
            source.sendFeedback(
                { "error.ledger.command.no_results".translate().setStyle(TextColorPallet.primary) },
                false
            )
            return
        }
        source.sendFeedback({ "text.ledger.header.search".translate().setStyle(TextColorPallet.secondary) }, false)
        results.forEach {
            source.sendFeedback({ it.toText() }, false)
        }
    }

    fun warnBusy(source: ServerCommandSource) {
//        if (DatabaseManager.dbMutex.isLocked) { //TODO
//            source.sendFeedback(
//                {
//                    Text.translatable(
//                        "text.ledger.database.busy"
//                    ).setStyle(TextColorPallet.primary)
//                },
//                false
//            )
//        }
    }

    @OptIn(ExperimentalTime::class)
    fun instantToText(time: Instant): MutableText {
        val duration = Duration.between(time, Instant.now()).toKotlinDuration()
        val text: MutableText = "".literal()

        duration.toComponents { days, hours, minutes, seconds, _ ->

            when {
                days > 0 -> text.append(days.toString()).append("d")
                hours > 0 -> text.append(hours.toString()).append("h")
                minutes > 0 -> text.append(minutes.toString()).append("m")
                else -> text.append(seconds.toString()).append("s")
            }
        }

        val message = Text.translatable("text.ledger.action_message.time_diff", text)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val timeMessage = formatter.format(time.atZone(TimeZone.getDefault().toZoneId())).literal()

        message.styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    timeMessage
                )
            )
        }
        return message
    }
}
