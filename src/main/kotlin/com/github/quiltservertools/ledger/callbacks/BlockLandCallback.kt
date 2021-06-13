package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockLandCallback {
    fun land(world: World, pos: BlockPos, state: BlockState)

    companion object {
        val EVENT: Event<BlockLandCallback> =
            EventFactory.createArrayBacked(BlockLandCallback::class.java) { listeners ->
                BlockLandCallback { world, pos, state ->
                    for (listener in listeners) {
                        listener.land(world, pos, state)
                    }
                }
            }
    }
}
