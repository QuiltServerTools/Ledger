package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface PlayerBlockPlaceCallback {
    fun place(
        world: World,
        player: PlayerEntity,
        pos: BlockPos,
        state: BlockState,
        context: ItemPlacementContext?,
        entity: BlockEntity?
    )

    companion object {
        @JvmField
        val EVENT: Event<PlayerBlockPlaceCallback> =
            EventFactory.createArrayBacked(PlayerBlockPlaceCallback::class.java) { listeners ->
                PlayerBlockPlaceCallback { world, player, pos, state, context, entity ->
                    for (listener in listeners) {
                        listener.place(world, player, pos, state, context, entity)
                    }
                }
            }
    }
}
