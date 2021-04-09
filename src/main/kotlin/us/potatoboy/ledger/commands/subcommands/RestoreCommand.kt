package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.server.command.CommandManager
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.launchMain

object RestoreCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("restore")
            .then(
                SearchParamArgument.argument("params")
                    .executes { restore(it, SearchParamArgument.get(it, "params")) })

            .build()
    }

    fun restore(context: Context, params: ActionSearchParams?): Int {
        val source = context.source

        if (params == null) return -1

        GlobalScope.launch(Dispatchers.IO) {
            val actions = DatabaseManager.restoreActions(params, source)

            if (actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            source.sendFeedback(
                TranslatableText(
                    "text.ledger.restore.start",
                    LiteralText(actions.size.toString()).setStyle(TextColorPallet.secondary)
                ).setStyle(TextColorPallet.primary), true
            )

            context.source.world.launchMain {
                for (action in actions) {
                    if (!action.restore(context.source.world)) {
                        //TODO deal with restores better
                        source.sendFeedback(
                            TranslatableText("text.ledger.restore.fail", action.objectIdentifier).setStyle(
                                TextColorPallet.secondary
                            ), true
                        )
                    }
                    action.rolledBack = true
                }

                source.sendFeedback(
                    TranslatableText(
                        "text.ledger.restore.finish",
                        actions.size
                    ).setStyle(TextColorPallet.primary), true
                )
            }
        }
        return 1
    }
}