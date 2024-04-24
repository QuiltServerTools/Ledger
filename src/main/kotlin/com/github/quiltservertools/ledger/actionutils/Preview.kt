package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.commands.subcommands.RestoreCommand
import com.github.quiltservertools.ledger.commands.subcommands.RollbackCommand
import com.github.quiltservertools.ledger.mixin.preview.EntityTrackerEntryAccessor
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.TextColorPallet
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.EntityTrackerEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

class Preview(
    private val params: ActionSearchParams,
    actions: List<ActionType>,
    player: ServerPlayerEntity,
    private val type: Type
) {
    val positions = mutableSetOf<BlockPos>()

    // Preview entities that got spawned. Need to removed
    val spawnedEntityTrackers = mutableSetOf<EntityTrackerEntry>()

    // Preview entities that got removed. Need to be spawned
    val removedEntityTrackers = mutableSetOf<EntityTrackerEntry>()

    // Preview items that should be modified in screen handlers (true = added, false = removed)
    val modifiedItems = mutableMapOf<BlockPos, MutableList<Pair<ItemStack, Boolean>>>()

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
                Type.ROLLBACK -> action.previewRollback(this, player)
                Type.RESTORE -> action.previewRestore(this, player)
            }
        }
    }

    fun cancel(player: ServerPlayerEntity) {
        for (pos in positions) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(player.world, pos))
        }
        cleanup(player)
    }

    private fun cleanup(player: ServerPlayerEntity) {
        // Cleanup preview entities, to keep client and server in sync
        spawnedEntityTrackers.forEach {
            if (!isEntityPresent(it)) {
                it.stopTracking(player)
            }
        }
        removedEntityTrackers.forEach {
            if (isEntityPresent(it)) {
                it.startTracking(player)
            }
        }
    }

    private fun isEntityPresent(entityTrackerEntry: EntityTrackerEntry): Boolean {
        val entity = (entityTrackerEntry as EntityTrackerEntryAccessor).entity
        return entity.world.getEntityById(entity.id) != null
    }

    fun apply(context: Context) {
        cleanup(context.source.playerOrThrow)
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
