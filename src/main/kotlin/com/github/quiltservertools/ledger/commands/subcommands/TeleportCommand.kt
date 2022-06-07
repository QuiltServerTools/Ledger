package com.github.quiltservertools.ledger.commands.subcommands

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.command.argument.PosArgument
import net.minecraft.command.argument.Vec3ArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.world.ServerWorld
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode

object TeleportCommand : BuildableCommand {
    override fun build(): LiteralNode =
        CommandManager.literal("tp")
            .requires(Permissions.require("ledger.commands.tp", CommandConsts.PERMISSION_LEVEL))
            .then(
                CommandManager.argument("world", DimensionArgumentType.dimension())
                    .then(
                        CommandManager.argument("location", Vec3ArgumentType.vec3())
                            .executes {
                                teleport(
                                    it,
                                    DimensionArgumentType.getDimensionArgument(it, "world"),
                                    Vec3ArgumentType.getPosArgument(it, "location")
                                )
                            }
                    )
            )
            .build()

    private fun teleport(context: Context, world: ServerWorld, posArg: PosArgument): Int {
        val player = context.source.playerOrThrow
        val pos = posArg.toAbsolutePos(context.source)
        player.teleport(world, pos.x, pos.y, pos.z, player.yaw, player.pitch)

        return 1
    }
}
