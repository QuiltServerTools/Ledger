package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

object RollbackCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("rollback")
            .then(
                SearchParamArgument.argument("params")
                    .executes { rollback(it, SearchParamArgument.get(it, "params")) })

            .build()
    }

    fun rollback(context: Context, params: ActionSearchParams?): Int {
        val source = context.source

        if (params == null) return -1

        GlobalScope.launch(Dispatchers.IO) {
            val actions = DatabaseManager.rollbackActions(params, source)

            if (actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            source.sendFeedback(TranslatableText("text.ledger.rollback.start", actions.size), true)
            for (action in actions) {
                if (!action.rollback(context.source.world)) {
                    source.sendFeedback(TranslatableText("text.ledger.rollback.fail", action.objectIdentifier), true)
                }
                action.rolledBack = true
            }
            source.sendFeedback(TranslatableText("text.ledger.rollback.finish", actions.size), true)

        }
        return 1
    }
}