package com.github.quiltservertools.ledger.actionutils

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.commands.subcommands.RestoreCommand
import com.github.quiltservertools.ledger.commands.subcommands.RollbackCommand
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.TextColorPallet

class Preview(
    private val params: ActionSearchParams,
    actions: List<ActionType>,
    player: ServerPlayerEntity,
    private val type: Type
) {
    private val positions = mutableSetOf<BlockPos>()

    init {
        player.sendMessage(
            TranslatableText(
                "text.ledger.preview.start",
                actions.size
            ).setStyle(TextColorPallet.primary),
            false
        )

        for (action in actions) {
            when (type) {
                Type.ROLLBACK -> action.previewRollback(player.world as ServerWorld, player)
                Type.RESTORE -> action.previewRestore(player.world as ServerWorld, player)
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
