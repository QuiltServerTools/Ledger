package us.potatoboy.ledger.utility

import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.actionutils.SearchResults
import us.potatoboy.ledger.database.DatabaseManager

object MessageUtils {
    fun sendSearchResults(source: ServerCommandSource, results: SearchResults, header: Text) {
        source.sendFeedback(header, false)

        results.actions.forEach { actionType ->
            source.sendFeedback(actionType.getMessage(), false)
        }

        source.sendFeedback(
            TranslatableText(
                "text.ledger.footer.search",
                TranslatableText("text.ledger.footer.page_backward").setStyle(TextColorPallet.primaryVariant).styled {
                    if (results.page > 1) {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                TranslatableText("text.ledger.footer.page_backward.hover")
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
                TranslatableText("text.ledger.footer.page_forward").setStyle(TextColorPallet.primaryVariant).styled {
                    if (results.page < results.pages) {
                        it.withHoverEvent(
                            HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                TranslatableText("text.ledger.footer.page_forward.hover")
                            )
                        ).withClickEvent(
                            ClickEvent(ClickEvent.Action.RUN_COMMAND, "/lg pg ${results.page + 1}")
                        )
                    } else {
                        Style.EMPTY
                    }
                }
            ).setStyle(TextColorPallet.primary),
            false
        )
    }

    fun warnBusy(source: ServerCommandSource) {
        if (DatabaseManager.dbMutex.isLocked) {
            source.sendFeedback(
                TranslatableText(
                    "text.ledger.database.busy"
                ).setStyle(TextColorPallet.primary),
                false
            )
        }
    }
}
