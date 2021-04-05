package us.potatoboy.ledger.actionutils

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.commands.subcommands.RollbackCommand
import us.potatoboy.ledger.utility.Context

class Preview(private val params: ActionSearchParams, actions: List<ActionType>, player: ServerPlayerEntity) {
    private val positions = mutableSetOf<BlockPos>()

    init {
        player.sendMessage(TranslatableText("text.ledger.preview.start", actions.size).setStyle(TextColorPallet.primary), false)

        for (action in actions) {
            action.preview(player.world as ServerWorld, player)
            positions.add(action.pos)
        }
    }

    fun cancel(player: ServerPlayerEntity) {
        for (pos in positions) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(player.world, pos))
        }
    }

    fun apply(context: Context) {
        RollbackCommand.rollback(context, params)
    }
}