package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.GameProfileArgument
import net.minecraft.server.players.NameAndId

object PlayerCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("player")
            .requires(Permissions.require("ledger.commands.player", CommandConsts.PERMISSION_LEVEL))
            .then(
                argument("player", GameProfileArgument.gameProfile())
                .executes {
                    return@executes lookupPlayer(GameProfileArgument.getGameProfiles(it, "player"), it.source)
                }
            )
            .build()
    }

    private fun lookupPlayer(profiles: MutableCollection<NameAndId>, source: CommandSourceStack): Int {
        Ledger.launch {
            val players = DatabaseManager.searchPlayers(profiles.toSet())
            MessageUtils.sendPlayerMessage(source, players)
        }

        return 1
    }
}
