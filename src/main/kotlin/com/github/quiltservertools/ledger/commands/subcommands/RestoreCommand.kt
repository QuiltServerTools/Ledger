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
import com.github.quiltservertools.ledger.utility.launchMain
import com.github.quiltservertools.ledger.utility.literal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText

object RestoreCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("restore")
            .requires(Permissions.require("ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL))
            .then(
                SearchParamArgument.argument("params")
                    .executes { restore(it, SearchParamArgument.get(it, "params")) }
            )
            .build()
    }

    fun restore(context: Context, params: ActionSearchParams): Int {
        val source = context.source

        Ledger.launch(Dispatchers.IO) {
            MessageUtils.warnBusy(source)
            val actions = DatabaseManager.restoreActions(params)

            if (actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            source.sendFeedback(
                TranslatableText(
                    "text.ledger.restore.start",
                    actions.size.toString().literal().setStyle(TextColorPallet.secondary)
                ).setStyle(TextColorPallet.primary),
                true
            )

            context.source.world.launchMain {
                val fails = HashMap<String, Int>()
                for (action in actions) {
                    if (!action.restore(context.source.server)) {
                        fails[action.identifier] = fails.getOrPut(action.identifier) { 0 } + 1
                    }
                    action.rolledBack = true
                }

                for (entry in fails.entries) {
                    source.sendFeedback(
                        TranslatableText("text.ledger.restore.fail", entry.key, entry.value).setStyle(
                            TextColorPallet.secondary
                        ),
                        true
                    )
                }

                source.sendFeedback(
                    TranslatableText(
                        "text.ledger.restore.finish",
                        actions.size
                    ).setStyle(TextColorPallet.primary),
                    true
                )
            }
        }
        return 1
    }
}
