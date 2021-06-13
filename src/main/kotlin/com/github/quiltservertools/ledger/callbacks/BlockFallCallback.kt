package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockFallCallback {
    fun fall(world: World, pos: BlockPos, state: BlockState)

    companion object {
        val EVENT: Event<BlockFallCallback> =
            EventFactory.createArrayBacked(BlockFallCallback::class.java) { listeners ->
                BlockFallCallback { world, pos, state ->
                    for (listener in listeners) {
                        listener.fall(world, pos, state)
                    }
                }
            }
    }
}
