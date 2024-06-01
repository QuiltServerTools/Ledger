package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text

object PurgeCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("purge")
            .requires(Permissions.require("ledger.commands.purge", config[SearchSpec.purgePermissionLevel]))
            .then(
                SearchParamArgument.argument(CommandConsts.PARAMS).executes {
                runPurge(it, SearchParamArgument.get(it, CommandConsts.PARAMS))
            }
            )
            .build()
    }

    private fun runPurge(ctx: Context, params: ActionSearchParams): Int {
        val source = ctx.source
        source.sendFeedback(
            { Text.translatable("text.ledger.purge.starting").setStyle(TextColorPallet.secondary) },
            true
        )
        Ledger.launch {
            DatabaseManager.purgeActions(params)
            source.sendFeedback(
                { Text.translatable("text.ledger.purge.complete").setStyle(TextColorPallet.secondary) },
                true
            )
        }
        return 1
    }
}
