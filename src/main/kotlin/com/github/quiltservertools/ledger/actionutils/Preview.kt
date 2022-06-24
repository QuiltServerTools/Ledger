package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.commands.subcommands.RestoreCommand
import com.github.quiltservertools.ledger.commands.subcommands.RollbackCommand
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.TextColorPallet
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

class Preview(
    private val params: ActionSearchParams,
    actions: List<ActionType>,
    player: ServerPlayerEntity,
    private val type: Type
) {
    private val positions = mutableSetOf<BlockPos>()

    init {
        player.sendMessage(
            Text.translatable(
                "text.ledger.preview.start",
                actions.size
            ).setStyle(TextColorPallet.primary),
            false
        )

        for (action in actions) {
            when (type) {
                Type.ROLLBACK -> action.previewRollback(player)
                Type.RESTORE -> action.previewRestore(player)
            }
            positions.add(action.pos)
        }
    }

    fun cancel(player: ServerPlayerEntity) {
        for (pos in positions) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(player.world, pos))
        }
    }

    fun apply(context: Context) {
        when (type) {
            Type.ROLLBACK -> RollbackCommand.rollback(context, params)
            Type.RESTORE -> RestoreCommand.restore(context, params)
        }
    }

    enum class Type {
        ROLLBACK,
        RESTORE
    }
}
