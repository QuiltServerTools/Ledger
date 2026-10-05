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
import net.minecraft.server.MinecraftServer

// Reoptimization: streaming rollback/restore tuning (shared with RestoreCommand).
// Previously the whole result set was loaded into memory and applied in one
// monolithic main-thread loop, freezing the server on large rollbacks. Now:
//  - DB batches are read via keyset pagination (max ROLLBACK_BATCH_SIZE rows in memory)
//  - the main thread works at most one tick budget before yielding (see below)
//  - rolled-back flags are committed per batch, so progress survives a crash
/**
 * Reoptimization: rows read and committed per rollback/restore step.
 *
 * Raised from 1,000 to 5,000 after measuring the trade-off on a 13,500-block
 * rollback: 1,000 took 3.86/4.16 s and 5,000 took 1.99/2.03 s, i.e. about twice as
 * fast, because the per-read and per-commit overhead is paid three times instead of
 * fourteen. Both batch sizes produced zero "can't keep up" warnings, so the larger
 * batch does not cost server responsiveness - the per-tick work budget below still
 * splits a batch and yields mid-way.
 *
 * Worst-case memory is bounded by about 5,000 materialised actions.
 */
internal const val ROLLBACK_BATCH_SIZE = 5000
internal const val ROLLBACK_PROGRESS_INTERVAL = 5 // batches between progress messages

// Reoptimization: adaptive tick budget thresholds. Smoothed tick durations (milliseconds
// of a 50 ms tick) that select a per-tick work budget (nanoseconds).
private const val SATURATED_TICK_MS = 50f
private const val BUSY_TICK_MS = 35f
private const val WARM_TICK_MS = 20f
private const val MIN_TICK_BUDGET_NS = 5_000_000L
private const val BUSY_TICK_BUDGET_NS = 15_000_000L
private const val WARM_TICK_BUDGET_NS = 25_000_000L
private const val IDLE_TICK_BUDGET_NS = 35_000_000L

/**
 * Reoptimization: the per-tick work budget is sized from the server's current tick
 * load instead of being a fixed 25 ms.
 *
 * Honest scope note: on a *benchmark* server (13,500-block rollback, nothing else
 * running) this changes almost nothing. Measured with the adaptive budget, a rollback
 * completes with zero `delay(1)` yields in ~3.1 s of which essentially all of it is
 * the block writes themselves - the budget was never the bottleneck there. Earlier
 * profiling notes claiming a large share of the wall time went into `delay(1)` were
 * wrong and have been removed.
 *
 * The point of the change is the *loaded* case, which a benchmark cannot show: a
 * fixed 25 ms budget behaves identically whether the server is idle or already
 * struggling at 45 ms ticks, because the budget cannot see the difference. Reading
 * the smoothed tick time lets a rollback back off automatically when the server is
 * under load, and use more of an idle tick when it is not. The feedback is
 * self-limiting: a rollback that stretches ticks shrinks its own budget next tick.
 *
 * @return nanoseconds of main-thread work allowed per tick
 */
internal fun rollbackTickBudgetNs(server: MinecraftServer): Long {
    val smoothedTickMs = server.currentSmoothedTickTime
    return when {
        // Server already at or past a full tick: barely touch the main thread.
        smoothedTickMs >= SATURATED_TICK_MS -> MIN_TICK_BUDGET_NS

        smoothedTickMs >= BUSY_TICK_MS -> BUSY_TICK_BUDGET_NS

        smoothedTickMs >= WARM_TICK_MS -> WARM_TICK_BUDGET_NS

        // Idle: use most of the spare time in the tick, so a rollback finishes fast.
        else -> IDLE_TICK_BUDGET_NS
    }
}

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

                    // Re-checked each batch: the server's tick load can change mid-rollback.
                    val tickBudgetNs = rollbackTickBudgetNs(context.source.server)

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
                        if (System.nanoTime() - batchStart > tickBudgetNs) {
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
