package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text

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
            source.sendError(Text.translatable("error.ledger.command.no_params"))
            return -1
        }

        Ledger.launch {
            Ledger.searchCache[source.name] = params

            MessageUtils.warnBusy(source)
            val results = DatabaseManager.searchActions(params, 1)

            if (results.actions.isEmpty()) {
                source.sendError(Text.translatable("error.ledger.command.no_results"))
                return@launch
            }

            MessageUtils.sendSearchResults(
                source,
                results,
                Text.translatable(
                    "text.ledger.header.search"
                ).setStyle(TextColorPallet.primary)
            )
        }

        return 1
    }
}
