package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface WorldBlockPlaceCallback {
    fun place(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: String
    )

    companion object {
        @JvmField
        val EVENT: Event<WorldBlockPlaceCallback> =
            EventFactory.createArrayBacked(WorldBlockPlaceCallback::class.java) { listeners ->
                WorldBlockPlaceCallback { world, pos, state, source ->
                    for (listener in listeners) {
                        listener.place(world, pos, state, source)
                    }
                }
            }
    }
}
