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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object RestoreCommand : BuildableCommand {
    override fun build(): LiteralNode = Commands.literal("restore")
        .requires(Permissions.require("ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL))
        .then(
            SearchParamArgument.argument("params")
                .executes { restore(it, SearchParamArgument.get(it, "params")) },
        )
        .build()

    fun restore(context: Context, params: ActionSearchParams): Int {
        val source = context.source
        params.ensureSpecific()
        Ledger.launch {
            MessageUtils.warnBusy(source)

            // Reoptimization: restore only targets previously rolled-back actions,
            // so no upper bound is needed - restoring logs new actions, but those
            // have rolledBack=false and the batch filter requires rolledBack=true.
            val total = DatabaseManager.countActionsFor(params, rolledBack = true)

            if (total == 0L) {
                source.sendFailure(Component.translatable("error.ledger.command.no_results"))
                return@launch
            }

            source.sendSuccess(
                {
                    Component.translatable(
                        "text.ledger.restore.start",
                        total.toString().literal().setStyle(TextColorPallet.secondary),
                    ).setStyle(TextColorPallet.primary)
                },
                true,
            )

            context.source.level.launchMain {
                val fails = HashMap<String, Int>()
                var applied = 0L
                var batchIndex = 0
                var cursor = 0

                while (true) {
                    // Keyset-paginated DB read: at most ROLLBACK_BATCH_SIZE rows in memory.
                    val actions = DatabaseManager.selectRestoreBatch(params, cursor, ROLLBACK_BATCH_SIZE)
                    if (actions.isEmpty()) break

                    var batchStart = System.nanoTime()
                    val actionIds = HashSet<Int>(actions.size)

                    for (action in actions) {
                        if (!action.restore(context.source.server)) {
                            fails[action.identifier] = fails.getOrPut(action.identifier) { 0 } + 1
                        } else {
                            actionIds.add(action.id)
                            applied++
                        }

                        // Adaptive tick budget: commit partial progress and yield the
                        // rest of the tick once we exceed the time budget.
                        if (System.nanoTime() - batchStart > ROLLBACK_TICK_BUDGET_NS) {
                            if (actionIds.isNotEmpty()) {
                                DatabaseManager.restoreActions(actionIds)
                                actionIds.clear()
                            }
                            delay(1) // one Minecraft tick
                            batchStart = System.nanoTime()
                        }
                    }

                    // Per-batch commit of the restored flags (crash-safe).
                    if (actionIds.isNotEmpty()) {
                        DatabaseManager.restoreActions(actionIds)
                    }

                    // Oldest-first ordering: the last row of the batch is the next cursor.
                    cursor = actions.last().id
                    batchIndex++

                    if (batchIndex % ROLLBACK_PROGRESS_INTERVAL == 0) {
                        source.sendSuccess(
                            {
                                Component.translatable(
                                    "text.ledger.restore.progress",
                                    applied.toString().literal().setStyle(TextColorPallet.secondary),
                                    total.toString().literal().setStyle(TextColorPallet.secondary),
                                ).setStyle(TextColorPallet.primary)
                            },
                            false,
                        )
                    }
                }

                for (entry in fails.entries) {
                    source.sendSuccess(
                        {
                            Component.translatable("text.ledger.restore.fail", entry.key, entry.value).setStyle(
                                TextColorPallet.secondary,
                            )
                        },
                        true,
                    )
                }

                source.sendSuccess(
                    {
                        Component.translatable(
                            "text.ledger.restore.finish",
                            applied.toString(),
                        ).setStyle(TextColorPallet.primary)
                    },
                    true,
                )
            }
        }
        return 1
    }
}
