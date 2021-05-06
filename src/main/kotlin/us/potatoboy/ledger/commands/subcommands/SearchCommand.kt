package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.MessageUtils
import us.potatoboy.ledger.utility.TextColorPallet

object SearchCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("search")
            .then(SearchParamArgument.argument("params")
                .executes { search(it, SearchParamArgument.get(it, "params")) })
            .build()
    }

    fun search(context: Context, params: ActionSearchParams): Int {
        val source = context.source

        if (params.isEmpty()) {
            source.sendError(TranslatableText("error.ledger.command.no_params"))
            return -1
        }

        runBlocking {
            launch(Dispatchers.IO) {
                Ledger.searchCache[source.name] = params

                val results = DatabaseManager.searchActions(params, 1, source)

                if (results.actions.isEmpty()) {
                    source.sendError(TranslatableText("error.ledger.command.no_results"))
                    return@launch
                }

                MessageUtils.sendSearchResults(
                    source, results,
                    TranslatableText(
                        "text.ledger.header.search"
                    ).setStyle(TextColorPallet.primary)
                )

            }
        }
        return 1
    }
}