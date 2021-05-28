package us.potatoboy.ledger.commands.subcommands

import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.*
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

object InspectCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("inspect")
            .requires { Permissions.check(it, "ledger.inspect", Ledger.PERMISSION_LEVEL) }
            .executes { toggleInspect(it) }
            .then(
                literal("on")
                    .executes { it.source.player.inspectOn() }
            )
            .then(
                literal("off")
                    .executes { it.source.player.inspectOff() }
            )
            .then(
                argument("pos", BlockPosArgumentType.blockPos())
                    .executes { inspectBlock(it, BlockPosArgumentType.getBlockPos(it, "pos")) }
            )
            .build()

    private fun toggleInspect(context: Context): Int {
        val source = context.source
        val player = source.player

        return if (player.isInspecting()) {
            player.inspectOff()
        } else {
            player.inspectOn()
        }
    }

    private fun inspectBlock(context: Context, pos: BlockPos): Int {
        val source = context.source

        source.player.inspectBlock(pos)
        return 1
    }
}
