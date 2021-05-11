package us.potatoboy.ledger.commands.subcommands

import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.InspectionManager
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

object InspectCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("inspect")
            .executes { toggleInspect(it) }
            .then(
                literal("on")
                    .executes { InspectionManager.inspectOn(it.source.player) }
            )
            .then(
                literal("off")
                    .executes { InspectionManager.inspectOff(it.source.player) }
            )
            .then(
                argument("pos", BlockPosArgumentType.blockPos())
                    .executes { inspectBlock(it, BlockPosArgumentType.getBlockPos(it, "pos")) }
            )
            .build()

    private fun toggleInspect(context: Context): Int {
        val source = context.source
        val player = source.player

        return if (InspectionManager.isInspecting(player)) {
            InspectionManager.inspectOff(player)
        } else {
            InspectionManager.inspectOn(player)
        }
    }

    private fun inspectBlock(context: Context, pos: BlockPos): Int {
        val source = context.source

        InspectionManager.inspectBlock(source.player, pos)
        return 1
    }
}
