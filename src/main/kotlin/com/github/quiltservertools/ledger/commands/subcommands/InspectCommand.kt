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
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.util.math.BlockPos

object InspectCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("inspect")
            .requires(Permissions.require("ledger.commands.inspect", CommandConsts.PERMISSION_LEVEL))
            .executes { toggleInspect(it) }
            .then(
                literal("on")
                    .executes { it.source.playerOrThrow.inspectOn() }
            )
            .then(
                literal("off")
                    .executes { it.source.playerOrThrow.inspectOff() }
            )
            .then(
                argument("pos", BlockPosArgumentType.blockPos())
                    .executes { inspectBlock(it, BlockPosArgumentType.getBlockPos(it, "pos")) }
            )
            .build()

    private fun toggleInspect(context: Context): Int {
        val source = context.source
        val player = source.playerOrThrow

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
