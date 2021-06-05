package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.CommandConsts
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.MessageUtils
import us.potatoboy.ledger.utility.TextColorPallet

object SearchCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("search")
            .requires(Permissions.require("ledger.commands.search", CommandConsts.PERMISSION_LEVEL))
            .then(
                SearchParamArgument.argument("params")
                    .executes { search(it, SearchParamArgument.get(it, "params")) }
            )
            .build()
    }

    private fun search(context: Context, params: ActionSearchParams): Int {
        val source = context.source
        if (params.isEmpty()) {
            source.sendError(TranslatableText("error.ledger.command.no_params"))
            return -1
        }

        Ledger.launch {
            Ledger.searchCache[source.name] = params

            MessageUtils.warnBusy(source)
            val results = DatabaseManager.searchActions(params, 1)

            if (results.actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            MessageUtils.sendSearchResults(
                source,
                results,
                TranslatableText(
                    "text.ledger.header.search"
                ).setStyle(TextColorPallet.primary)
            )
        }

        return 1
    }
}
