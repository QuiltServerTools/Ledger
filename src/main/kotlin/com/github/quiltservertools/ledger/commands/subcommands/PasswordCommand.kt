package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.translate
import com.github.quiltservertools.ledger.webui.WebUi
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import java.util.*

object PasswordCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("password")
            .then(argument("value", StringArgumentType.string())
                .requires(Permissions.require("ledger.commands.password", CommandConsts.PERMISSION_LEVEL))
                .executes {
                    resetPass(it.source, it.source.player.uuid, StringArgumentType.getString(it, "value"))
                })
            .build()
    }

    private fun resetPass(scs: ServerCommandSource, uuid: UUID, value: String): Int {
        Ledger.launch {
            scs.sendFeedback("text.ledger.password.updated".translate().setStyle(TextColorPallet.secondary), false)
            DatabaseManager.updatePlayerPassword(uuid, value)
            if (Permissions.check(scs, "ledger.commands.password.admin", CommandConsts.PERMISSION_LEVEL)) {
                // Player is admin
                DatabaseManager.updatePlayerPerms(uuid, CommandConsts.PERMISSION_LEVEL.toByte())
                WebUi.reloadUser(uuid)
            }
        }
        return 1
    }
}
