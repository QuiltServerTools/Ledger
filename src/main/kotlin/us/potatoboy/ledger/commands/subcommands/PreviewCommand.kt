package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.Preview
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

object PreviewCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return CommandManager.literal("preview")
            .then(
                SearchParamArgument.argument("params")
                    .executes { preview(it, SearchParamArgument.get(it, "params")) }
            )
            .then(CommandManager.literal("apply").executes { apply(it) })
            .then(CommandManager.literal("cancel").executes { cancel(it) })
            .build()
    }

    private fun preview(context: Context, params: ActionSearchParams?): Int {
        val source = context.source

        if (params == null) return -1

        Ledger.launch(Dispatchers.IO) {
            val actions = DatabaseManager.previewActions(params, source)

            if (actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }

            Ledger.previewCache[source.player.uuid] = Preview(params, actions, source.player)
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
