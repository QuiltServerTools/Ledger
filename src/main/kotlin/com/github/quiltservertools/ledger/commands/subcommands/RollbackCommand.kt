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
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text

object RollbackCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("rollback")
            .requires(Permissions.require("ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL))
            .then(
                SearchParamArgument.argument("params")
                    .executes { rollback(it, SearchParamArgument.get(it, "params")) }
            )
            .build()
    }

    fun rollback(context: Context, params: ActionSearchParams): Int {
        val source = context.source
        params.ensureSpecific()
        Ledger.launch {
            MessageUtils.warnBusy(source)
            val actions = DatabaseManager.selectRollback(params)

            if (actions.isEmpty()) {
                source.sendError(Text.translatable("error.ledger.command.no_results"))
                return@launch
            }

            source.sendFeedback(
                {
                    Text.translatable(
                        "text.ledger.rollback.start",
                        actions.size.toString().literal().setStyle(TextColorPallet.secondary)
                    ).setStyle(TextColorPallet.primary)
                },
                true
            )

            context.source.world.launchMain {
                val fails = HashMap<String, Int>()
                val actionIds = HashSet<Int>()
                for (action in actions) {
                    if (!action.rollback(context.source.server)) {
                        fails[action.identifier] = fails.getOrPut(action.identifier) { 0 } + 1
                    } else {
                        actionIds.add(action.id)
                    }
                }
                Ledger.launch {
                    DatabaseManager.rollbackActions(actionIds)
                }

                for (entry in fails.entries) {
                    source.sendFeedback(
                        {
                            Text.translatable("text.ledger.rollback.fail", entry.key, entry.value).setStyle(
                                TextColorPallet.secondary
                            )
                        },
                        true
                    )
                }

                source.sendFeedback(
                    {
                        Text.translatable(
                            "text.ledger.rollback.finish",
                            actions.size
                        ).setStyle(TextColorPallet.primary)
                    },
                    true
                )
            }
        }
        return 1
    }
}
