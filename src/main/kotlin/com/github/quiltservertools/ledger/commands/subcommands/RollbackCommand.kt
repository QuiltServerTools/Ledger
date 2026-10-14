package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ItemDespawnActionType
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
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import kotlin.time.ExperimentalTime

object RollbackCommand : BuildableCommand {
    override fun build(): LiteralNode = Commands.literal("rollback")
        .requires(Permissions.require("ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL))
        .then(
            SearchParamArgument.argument("params")
                .executes { rollback(it, SearchParamArgument.get(it, "params")) },
        )
        .build()

    /**
     * Rolls back exactly the one action behind a clicked search or inspect result. The action id
     * comes straight off a client packet, so the permission is re-checked here rather than relying
     * on the command node.
     */
    @OptIn(ExperimentalTime::class)
    fun rollbackSingle(player: ServerPlayer, actionId: Int): Int {
        if (!Permissions.check(player, "ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL)) return -1

        val source = player.createCommandSourceStack()

        Ledger.launch {
            MessageUtils.warnBusy(source)
            val action = DatabaseManager.selectRollbackById(actionId)

            if (action == null) {
                // The clicked chat line sticks around after a rollback, so a second click on it is
                // an expected case rather than a genuinely empty search.
                source.sendFailure(Component.translatable("error.ledger.rollback.unavailable"))
                return@launch
            }

            if (action is ItemDespawnActionType && !ItemDespawnActionType.canRollback(source)) {
                source.sendFailure(Component.translatable("error.ledger.rollback.item_despawn_permission"))
                return@launch
            }

            source.level.launchMain {
                if (!action.rollback(source.server)) {
                    source.sendFailure(
                        Component.translatable("text.ledger.rollback.fail", action.identifier, 1),
                    )
                    return@launchMain
                }

                Ledger.launch {
                    DatabaseManager.rollbackActions(setOf(action.id))
                }

                action.rolledBack = true
                source.sendSuccess(
                    {
                        Component.translatable(
                            "text.ledger.rollback.single",
                            action.getMessage(source),
                        ).setStyle(TextColorPallet.primary)
                    },
                    true,
                )
            }
        }

        return 1
    }

    fun rollback(context: Context, params: ActionSearchParams): Int {
        val source = context.source
        params.ensureSpecific()
        ItemDespawnActionType.restrictRollback(source, params)
        Ledger.launch {
            MessageUtils.warnBusy(source)
            val actions = DatabaseManager.selectRollback(params)

            if (actions.isEmpty()) {
                source.sendFailure(Component.translatable("error.ledger.command.no_results"))
                return@launch
            }

            source.sendSuccess(
                {
                    Component.translatable(
                        "text.ledger.rollback.start",
                        actions.size.toString().literal().setStyle(TextColorPallet.secondary),
                    ).setStyle(TextColorPallet.primary)
                },
                true,
            )

            context.source.level.launchMain {
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
                            actions.size,
                        ).setStyle(TextColorPallet.primary)
                    },
                    true,
                )
            }
        }
        return 1
    }
}
