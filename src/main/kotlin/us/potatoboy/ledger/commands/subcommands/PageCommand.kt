package us.potatoboy.ledger.commands.subcommands

import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.MessageUtils
import us.potatoboy.ledger.utility.TextColorPallet

object PageCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("page")
            .then(
                argument("page", IntegerArgumentType.integer(1))
                    .executes { page(it, IntegerArgumentType.getInteger(it, "page")) }
            )
            .build()

    private fun page(context: Context, page: Int): Int {
        val source = context.source

        val params = Ledger.searchCache[source.name]
        if (params != null) {
            Ledger.launch(Dispatchers.IO) {
                val results = DatabaseManager.searchActions(params, page, source)

                if (results.page > results.pages) {
                    source.sendError(TranslatableText("error.ledger.no_more_pages"))
                    return@launch
                }

                MessageUtils.sendSearchResults(
                    source, results,
                    TranslatableText(
                        "text.ledger.header.search"
                    ).setStyle(TextColorPallet.primary)
                )
            }

            return 1
        } else {
            source.sendError(TranslatableText("error.ledger.no_cached_params"))
            return -1
        }
    }
}
