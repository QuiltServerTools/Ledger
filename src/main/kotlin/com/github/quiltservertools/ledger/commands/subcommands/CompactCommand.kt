package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

/**
 * Reoptimization: /ledger compact
 *
 * Migrates legacy text block_state / old_block_state columns into the
 * block_states dictionary (int references) and then reclaims the freed disk
 * space with VACUUM (SQLite) / OPTIMIZE TABLE (MySQL).
 *
 * Safe to run on a live server:
 *  - reads and writes go through the single database thread
 *  - each row's text -> ref swap happens atomically in one UPDATE, so
 *    concurrent searches always see a valid state (either the text column or
 *    the ref column)
 *  - new writes already use the dictionary and are skipped automatically
 */
object CompactCommand : BuildableCommand {
    private const val PROGRESS_INTERVAL_MS = 5_000L

    override fun build(): LiteralNode = literal("compact")
        .requires(Permissions.require("ledger.commands.purge", config[SearchSpec.purgePermissionLevel]))
        .executes { runCompact(it) }
        .build()

    private fun runCompact(ctx: Context): Int {
        val source = ctx.source
        source.sendSuccess(
            { Component.translatable("text.ledger.compact.starting").setStyle(TextColorPallet.secondary) },
            true,
        )
        Ledger.launch {
            var lastReport = System.currentTimeMillis()
            val migrated = DatabaseManager.compactBlockStates(
                batchSize = 5000,
                onProgress = { done, total ->
                    val now = System.currentTimeMillis()
                    if (now - lastReport >= PROGRESS_INTERVAL_MS) {
                        lastReport = now
                        source.sendSuccess(
                            {
                                Component.translatable(
                                    "text.ledger.compact.progress",
                                    done.toString().literal().setStyle(TextColorPallet.secondary),
                                    total.toString().literal().setStyle(TextColorPallet.secondary),
                                ).setStyle(TextColorPallet.primary)
                            },
                            false,
                        )
                    }
                },
            )
            DatabaseManager.vacuumDatabase()

            source.sendSuccess(
                {
                    Component.translatable(
                        "text.ledger.compact.complete",
                        migrated.toString().literal().setStyle(TextColorPallet.secondary),
                    ).setStyle(TextColorPallet.primary)
                },
                true,
            )
        }
        return 1
    }
}
