package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText

object PurgeCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("purge")
            .requires(Permissions.require("ledger.command.purge", config[SearchSpec.purgePermissionLevel]))
            .then(SearchParamArgument.argument(CommandConsts.PARAMS).executes {
                runPurge(it, SearchParamArgument.get(it, CommandConsts.PARAMS))
            })
            .build()
    }

    private fun runPurge(ctx: CommandContext<ServerCommandSource>, params: ActionSearchParams): Int {
        val source = ctx.source
        source.sendFeedback(TranslatableText("text.ledger.purge.starting").setStyle(TextColorPallet.secondary), false)
        Ledger.launch {
            DatabaseManager.purge(params)
            source.sendFeedback(TranslatableText("text.ledger.purge.complete").setStyle(TextColorPallet.secondary), true)
        }
        return 1
    }
}
