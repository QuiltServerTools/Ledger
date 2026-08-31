package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.permissions.Permissions
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.DimensionArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

object TeleportCommand : BuildableCommand {
    private const val BLOCK_CENTER_OFFSET = 0.5
    override fun build(): LiteralNode = Commands.literal("tp")
        .requires(Permissions.has(Permissions.TP))
        .then(
            Commands.argument("world", DimensionArgument.dimension())
                .then(
                    Commands.argument("location", Vec3Argument.vec3())
                        .executes {
                            teleport(
                                it,
                                DimensionArgument.getDimension(it, "world"),
                                Vec3Argument.getCoordinates(it, "location"),
                            )
                        },
                ),
        )
        .build()

    private fun teleport(context: Context, world: ServerLevel, posArg: Coordinates): Int {
        val player = context.source.playerOrException
        val pos = posArg.getBlockPos(context.source)

        teleport(player, world, pos)

        return 1
    }

    fun teleport(player: ServerPlayer, world: ServerLevel, pos: BlockPos) {
        if (!Permissions.check(player, Permissions.TP)) return

        val x = pos.x.toDouble() + BLOCK_CENTER_OFFSET
        val z = pos.z.toDouble() + BLOCK_CENTER_OFFSET

        player.teleportTo(
            world,
            x,
            pos.y.toDouble(),
            z,
            emptySet(),
            player.yRot,
            player.xRot,
            true,
        )
    }
}
