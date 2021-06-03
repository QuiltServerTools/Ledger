package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.CommandConsts
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.launchMain
import us.potatoboy.ledger.utility.literal

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
            val actions = DatabaseManager.restoreActions(params, source)

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
                    if (!action.restore(context.source.world)) {
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
