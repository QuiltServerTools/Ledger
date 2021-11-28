package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockMeltCallback {
    fun melt(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, entity: BlockEntity?)

    companion object {
        @JvmField
        val EVENT: Event<BlockMeltCallback> =
            EventFactory.createArrayBacked(BlockMeltCallback::class.java) { listeners ->
                BlockMeltCallback { world, pos, oldState, newState, entity ->
                    for (listener in listeners) {
                        listener.melt(world, pos, oldState, newState, entity)
                    }
                }
            }
    }
}
