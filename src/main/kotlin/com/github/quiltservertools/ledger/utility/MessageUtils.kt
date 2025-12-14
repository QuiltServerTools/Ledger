package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.Networking.hasNetworking
import com.github.quiltservertools.ledger.network.packet.action.ActionS2CPacket
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

object MessageUtils {
    @OptIn(ExperimentalTime::class)
    suspend fun sendSearchResults(source: CommandSourceStack, results: SearchResults, header: Component) {
        // If the player has a Ledger compatible client, we send results as action packets rather than as chat messages
        if (source.hasPlayer() && source.playerOrException.hasNetworking()) {
            for (n in results.page..results.pages) {
                val networkResults = DatabaseManager.searchActions(results.searchParams, n)
                networkResults.actions.forEach {
                    ServerPlayNetworking.send(source.playerOrException, ActionS2CPacket(it))
                }
            }
            return
        }

        source.sendSystemMessage(header)

        results.actions.forEach { actionType ->
            source.sendSystemMessage(actionType.getMessage(source))
        }

        source.sendSystemMessage(
            Component.translatable(
                "text.ledger.footer.search",
                Component.translatable(
                    "text.ledger.footer.page_backward"
                ).setStyle(TextColorPallet.primaryVariant).withStyle {
                    if (results.page > 1) {
                        it.withHoverEvent(
                            HoverEvent.ShowText(
                                Component.translatable("text.ledger.footer.page_backward.hover")
                            )
                        ).withClickEvent(
                            ClickEvent.RunCommand("/lg pg ${results.page - 1}")
                        )
                    } else {
                        Style.EMPTY
                    }
                },
                results.page.toString().literal().setStyle(TextColorPallet.primaryVariant),
                results.pages.toString().literal().setStyle(TextColorPallet.primaryVariant),
                Component.translatable(
                    "text.ledger.footer.page_forward"
                ).setStyle(TextColorPallet.primaryVariant).withStyle {
                    if (results.page < results.pages) {
                        it.withHoverEvent(
                            HoverEvent.ShowText(
                                Component.translatable("text.ledger.footer.page_forward.hover")
                            )
                        ).withClickEvent(
                            ClickEvent.RunCommand("/lg pg ${results.page + 1}")
                        )
                    } else {
                        Style.EMPTY
                    }
                }
            ).setStyle(TextColorPallet.primary)
        )
    }

    fun sendPlayerMessage(source: CommandSourceStack, results: List<PlayerResult>) {
        if (results.isEmpty()) {
            source.sendSystemMessage("error.ledger.command.no_results".translate().setStyle(TextColorPallet.primary))
            return
        }
        source.sendSystemMessage("text.ledger.header.search".translate().setStyle(TextColorPallet.secondary))
        results.forEach {
            source.sendSystemMessage(it.toText())
        }
    }

    fun warnBusy(source: CommandSourceStack) {
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

    fun instantToText(time: Instant): MutableComponent {
        val duration = Duration.between(time, Instant.now()).toKotlinDuration()
        val text: MutableComponent = "".literal()

        duration.toComponents { days, hours, minutes, seconds, _ ->

            when {
                days > 0 -> text.append(days.toString()).append("d")
                hours > 0 -> text.append(hours.toString()).append("h")
                minutes > 0 -> text.append(minutes.toString()).append("m")
                else -> text.append(seconds.toString()).append("s")
            }
        }

        val message = Component.translatable("text.ledger.action_message.time_diff", text)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val timeMessage = formatter.format(time.atZone(Ledger.config[SearchSpec.timeZone])).literal()

        message.withStyle {
            it.withHoverEvent(
                HoverEvent.ShowText(
                    timeMessage
                )
            )
        }
        return message
    }
}
