package com.github.quiltservertools.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText
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

object PreviewCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("preview")
            .requires(Permissions.require("ledger.commands.preview", CommandConsts.PERMISSION_LEVEL))
            .then(CommandManager.literal("rollback")
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
            .then(CommandManager.literal("restore")
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
            .then(CommandManager.literal("apply").executes { apply(it) })
            .then(CommandManager.literal("cancel").executes { cancel(it) })
            .build()
    }

    private fun preview(context: Context, params: ActionSearchParams?, type: Preview.Type): Int {
        val source = context.source

        if (params == null) return -1

        Ledger.launch(Dispatchers.IO) {
            MessageUtils.warnBusy(source)
            val actions = DatabaseManager.previewActions(params, type)

            if (actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            Ledger.previewCache[source.player.uuid] = Preview(params, actions, source.player, type)
        }
        return 1
    }

    private fun apply(context: Context): Int {
        val uuid = context.source.player.uuid

        if (Ledger.previewCache.containsKey(uuid)) {
            Ledger.previewCache[uuid]?.apply(context)
        } else {
            context.source.sendError(TranslatableText("error.ledger.no_preview"))
            Ledger.previewCache.remove(uuid)
            return -1
        }

        return 1
    }

    private fun cancel(context: Context): Int {
        val uuid = context.source.player.uuid

        if (Ledger.previewCache.containsKey(uuid)) {
            Ledger.previewCache[uuid]?.cancel(context.source.player)
            Ledger.previewCache.remove(uuid)
        } else {
            context.source.sendError(TranslatableText("error.ledger.no_preview"))
            return -1
        }

        return 1
    }
}
