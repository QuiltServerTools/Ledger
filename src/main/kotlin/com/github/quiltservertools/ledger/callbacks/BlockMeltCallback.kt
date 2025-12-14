package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

fun interface BlockMeltCallback {
    fun melt(world: Level, pos: BlockPos, oldState: BlockState, newState: BlockState, entity: BlockEntity?)

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
