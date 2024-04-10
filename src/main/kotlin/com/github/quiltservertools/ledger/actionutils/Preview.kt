package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.commands.subcommands.RestoreCommand
import com.github.quiltservertools.ledger.commands.subcommands.RollbackCommand
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.TextColorPallet
import java.util.*
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.BundleS2CPacket
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
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
    val spawnedEntityIds = mutableSetOf<Int>()
    // Preview entities that got removed. Need to be spawned
    val removedEntityUuids = mutableSetOf<UUID>()
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
        val destroyPackets = spawnedEntityIds.map {
            EntitiesDestroyS2CPacket(it)
        }
        player.networkHandler.sendPacket(BundleS2CPacket(destroyPackets))

        spawnedEntityIds.forEach {
            player.networkHandler.sendPacket(EntitiesDestroyS2CPacket(it))
        }
        val spawnPackets = removedEntityUuids.mapNotNull {
            val world = player.serverWorld
            val entity = world?.getEntity(it)
            entity?.let {
                EntitySpawnS2CPacket(entity)
            }
        }
        player.networkHandler.sendPacket(BundleS2CPacket(spawnPackets))
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
