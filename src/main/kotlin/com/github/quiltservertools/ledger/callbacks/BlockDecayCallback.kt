package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockDecayCallback {
    fun decay(world: World, pos: BlockPos, state: BlockState, entity: BlockEntity?)

    companion object {
        @JvmField
        val EVENT: Event<BlockDecayCallback> =
            EventFactory.createArrayBacked(BlockDecayCallback::class.java) { listeners ->
                BlockDecayCallback { world, pos, state, entity ->
                    for (listener in listeners) {
                        listener.decay(world, pos, state, entity)
                    }
                }
            }
    }
}
