package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockBurnCallback {
    fun burn(world: World, pos: BlockPos, state: BlockState, entity: BlockEntity?)

    companion object {
        val EVENT: Event<BlockBurnCallback> =
            EventFactory.createArrayBacked(BlockBurnCallback::class.java) { listeners ->
                BlockBurnCallback { world, pos, state, entity ->
                    for (listener in listeners) {
                        listener.burn(world, pos, state, entity)
                    }
                }
            }
    }
}
