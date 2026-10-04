package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.PARAMS
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.permissions.Permissions
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import kotlinx.coroutines.launch
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

object PurgeCommand : BuildableCommand {
    override fun build(): LiteralNode = literal("purge")
        .requires(Permissions.has(Permissions.PURGE))
        .then(
            SearchParamArgument.argument(PARAMS).executes {
                runPurge(it, SearchParamArgument.get(it, PARAMS))
            },
        )
        .build()

    private fun runPurge(ctx: Context, params: ActionSearchParams): Int {
        val source = ctx.source
        source.sendSuccess(
            { Component.translatable("text.ledger.purge.starting").setStyle(TextColorPallet.secondary) },
            true,
        )
        Ledger.launch {
            DatabaseManager.purgeActions(params)
            source.sendSuccess(
                { Component.translatable("text.ledger.purge.complete").setStyle(TextColorPallet.secondary) },
                true,
            )
        }
        return 1
    }
}
