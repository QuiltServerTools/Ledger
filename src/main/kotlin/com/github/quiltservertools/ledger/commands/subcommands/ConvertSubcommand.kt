package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

private const val PROGRESS_INTERVAL = 30L

object ConvertSubcommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("convert")
            .requires(Permissions.require("ledger.commands.convert", CommandConsts.PERMISSION_LEVEL))
            .executes { convertDatabase(it) }
            .build()

    private fun convertDatabase(it: CommandContext<ServerCommandSource>): Int {
        Ledger.launch {
            DatabaseManager.convertActions { done, total ->
                if (done % PROGRESS_INTERVAL == 0L) {
                    it.source.sendFeedback({ Text.of("Converted $done/$total actions") }, false)
                }
            }
        }
        return 1
    }
}
