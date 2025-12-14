package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.inspectBlock
import com.github.quiltservertools.ledger.utility.inspectOff
import com.github.quiltservertools.ledger.utility.inspectOn
import com.github.quiltservertools.ledger.utility.isInspecting
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.core.BlockPos

object InspectCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("inspect")
            .requires(Permissions.require("ledger.commands.inspect", CommandConsts.PERMISSION_LEVEL))
            .executes { toggleInspect(it) }
            .then(
                literal("on")
                    .executes { it.source.playerOrException.inspectOn() }
            )
            .then(
                literal("off")
                    .executes { it.source.playerOrException.inspectOff() }
            )
            .then(
                argument("pos", BlockPosArgument.blockPos())
                    .executes { inspectBlock(it, BlockPosArgument.getBlockPos(it, "pos")) }
            )
            .build()

    private fun toggleInspect(context: Context): Int {
        val source = context.source
        val player = source.playerOrException

        return if (player.isInspecting()) {
            player.inspectOff()
        } else {
            player.inspectOn()
        }
    }

    private fun inspectBlock(context: Context, pos: BlockPos): Int {
        val source = context.source

        source.inspectBlock(pos)
        return 1
    }
}
