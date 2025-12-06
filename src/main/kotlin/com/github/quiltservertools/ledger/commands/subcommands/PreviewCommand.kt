package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object PreviewCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return Commands.literal("preview")
            .requires(Permissions.require("ledger.commands.preview", CommandConsts.PERMISSION_LEVEL))
            .then(
                Commands.literal("rollback")
                .then(
                    SearchParamArgument.argument(CommandConsts.PARAMS)
                        .executes {
                            preview(
                                it,
                                SearchParamArgument.get(it, CommandConsts.PARAMS),
                                Preview.Type.ROLLBACK
                            )
                        }
                )
            )
            .then(
                Commands.literal("restore")
                .then(
                    SearchParamArgument.argument(CommandConsts.PARAMS)
                        .executes {
                            preview(
                                it,
                                SearchParamArgument.get(it, CommandConsts.PARAMS),
                                Preview.Type.RESTORE
                            )
                        }
                )
            )
            .then(Commands.literal("apply").executes { apply(it) })
            .then(Commands.literal("cancel").executes { cancel(it) })
            .build()
    }

    private fun preview(context: Context, params: ActionSearchParams, type: Preview.Type): Int {
        val source = context.source
        val player = source.playerOrException
        params.ensureSpecific()
        Ledger.launch {
            MessageUtils.warnBusy(source)
            val actions = DatabaseManager.previewActions(params, type)

            if (actions.isEmpty()) {
                source.sendFailure(Component.translatable("error.ledger.command.no_results"))
                return@launch
            }

            Ledger.previewCache[player.uuid]?.cancel(player)
            Ledger.previewCache[player.uuid] = Preview(params, actions, player, type)
        }
        return 1
    }

    private fun apply(context: Context): Int {
        val uuid = context.source.playerOrException.uuid

        if (Ledger.previewCache.containsKey(uuid)) {
            Ledger.previewCache[uuid]?.apply(context)
            Ledger.previewCache.remove(uuid)
        } else {
            context.source.sendFailure(Component.translatable("error.ledger.no_preview"))
            return -1
        }

        return 1
    }

    private fun cancel(context: Context): Int {
        val uuid = context.source.playerOrException.uuid

        if (Ledger.previewCache.containsKey(uuid)) {
            Ledger.previewCache[uuid]?.cancel(context.source.playerOrException)
            Ledger.previewCache.remove(uuid)
        } else {
            context.source.sendFailure(Component.translatable("error.ledger.no_preview"))
            return -1
        }

        return 1
    }
}
