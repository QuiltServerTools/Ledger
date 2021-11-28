package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.argument.GameProfileArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource

object PlayerCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("player")
            .requires(Permissions.require("ledger.commands.player", CommandConsts.PERMISSION_LEVEL))
            .then(argument("player", GameProfileArgumentType.gameProfile())
                .executes {
                    return@executes lookupPlayer(GameProfileArgumentType.getProfileArgument(it, "player"), it.source)
                })
            .build()
    }

    private fun lookupPlayer(profiles: MutableCollection<GameProfile>, source: ServerCommandSource): Int {

        Ledger.launch {
            val players = DatabaseManager.searchPlayers(profiles.toSet())
            MessageUtils.sendPlayerMessage(source, players)
        }

        return 1
    }
}
