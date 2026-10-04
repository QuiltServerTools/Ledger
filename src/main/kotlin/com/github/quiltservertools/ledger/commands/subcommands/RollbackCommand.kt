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

// Reoptimization: streaming rollback/restore tuning (shared with RestoreCommand).
// Previously the whole result set was loaded into memory and applied in one
// monolithic main-thread loop, freezing the server on large rollbacks. Now:
//  - DB batches are read via keyset pagination (max ROLLBACK_BATCH_SIZE rows in memory)
//  - the main thread works at most ROLLBACK_TICK_BUDGET_MS before yielding a tick
//  - rolled-back flags are committed per batch, so progress survives a crash
internal const val ROLLBACK_BATCH_SIZE = 1000
internal const val ROLLBACK_TICK_BUDGET_NS = 25_000_000L // 25ms = half a Minecraft tick (50ms)
internal const val ROLLBACK_PROGRESS_INTERVAL = 5 // batches between progress messages

object RollbackCommand : BuildableCommand {
    override fun build(): LiteralNode = Commands.literal("rollback")
        .requires(Permissions.require("ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL))
        .then(
            SearchParamArgument.argument("params")
                .executes { rollback(it, SearchParamArgument.get(it, "params")) },
        )
        .build()

    fun rollback(context: Context, params: ActionSearchParams): Int {
        val source = context.source
        params.ensureSpecific()
        Ledger.launch {
            MessageUtils.warnBusy(source)

            // Reoptimization: exclusive upper bound taken before any changes are applied,
            // so actions logged by this rollback itself (ids >= bound) can never be
            // picked up by later batches of the same rollback. +1 because the keyset
            // predicate is strictly less-than and the newest row must be included.
            val upperBound = DatabaseManager.currentMaxActionId() + 1
            val total = DatabaseManager.countActionsFor(params, rolledBack = false)

            if (total == 0L) {
                source.sendFailure(Component.translatable("error.ledger.command.no_results"))
                return@launch
            }

            source.sendSuccess(
                {
                    Component.translatable(
                        "text.ledger.rollback.start",
                        total.toString().literal().setStyle(TextColorPallet.secondary),
                    ).setStyle(TextColorPallet.primary)
                },
                true,
            )

            context.source.level.launchMain {
                val fails = HashMap<String, Int>()
                var applied = 0L
                var batchIndex = 0
                var cursor = upperBound

                while (true) {
                    // Keyset-paginated DB read: at most ROLLBACK_BATCH_SIZE rows in memory.
                    val actions = DatabaseManager.selectRollbackBatch(params, cursor, ROLLBACK_BATCH_SIZE)
                    if (actions.isEmpty()) break

                    var batchStart = System.nanoTime()
                    val actionIds = HashSet<Int>(actions.size)

                    for (action in actions) {
                        if (!action.rollback(context.source.server)) {
                            fails[action.identifier] = fails.getOrPut(action.identifier) { 0 } + 1
                        } else {
                            actionIds.add(action.id)
                            applied++
                        }

                        // Adaptive tick budget: commit partial progress and yield the
                        // rest of the tick once we exceed the time budget.
                        if (System.nanoTime() - batchStart > ROLLBACK_TICK_BUDGET_NS) {
                            if (actionIds.isNotEmpty()) {
                                DatabaseManager.rollbackActions(actionIds)
                                actionIds.clear()
                            }
                            delay(1) // one Minecraft tick
                            batchStart = System.nanoTime()
                        }
                    }

                    // Per-batch commit of the rolled-back flags (crash-safe).
                    if (actionIds.isNotEmpty()) {
                        DatabaseManager.rollbackActions(actionIds)
                    }

                    // Newest-first ordering: the last row of the batch is the next cursor.
                    cursor = actions.last().id
                    batchIndex++

                    if (batchIndex % ROLLBACK_PROGRESS_INTERVAL == 0) {
                        source.sendSuccess(
                            {
                                Component.translatable(
                                    "text.ledger.rollback.progress",
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
                            Component.translatable("text.ledger.rollback.fail", entry.key, entry.value).setStyle(
                                TextColorPallet.secondary,
                            )
                        },
                        true,
                    )
                }

                source.sendSuccess(
                    {
                        Component.translatable(
                            "text.ledger.rollback.finish",
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
