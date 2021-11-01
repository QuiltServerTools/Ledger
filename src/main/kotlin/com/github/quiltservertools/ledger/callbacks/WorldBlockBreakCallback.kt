package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface WorldBlockBreakCallback {
    fun place(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: String,
        player: PlayerEntity?
    )

    companion object {
        @JvmField
        val EVENT: Event<WorldBlockPlaceCallback> =
            EventFactory.createArrayBacked(WorldBlockPlaceCallback::class.java) { listeners ->
                WorldBlockPlaceCallback { world, pos, state, source, player ->
                    for (listener in listeners) {
                        listener.place(world, pos, state, source, player)
                    }
                }
            }
    }
}
