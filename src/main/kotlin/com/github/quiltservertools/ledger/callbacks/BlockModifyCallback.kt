package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockModifyCallback {
    fun modify(
        world: World,
        entity: Entity,
        pos: BlockPos,
        state: BlockState
    )

    companion object {
        val EVENT: Event<BlockModifyCallback> =
            EventFactory.createArrayBacked(BlockModifyCallback::class.java) { listeners ->
                BlockModifyCallback { world, entity, pos, state ->
                    for (listener in listeners) {
                        listener.modify(world, entity, pos, state)
                    }
                }
            }
    }
}
