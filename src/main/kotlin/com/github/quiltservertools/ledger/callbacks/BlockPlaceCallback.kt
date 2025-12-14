package com.github.quiltservertools.ledger.callbacks

import com.github.quiltservertools.ledger.utility.Sources
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

fun interface BlockPlaceCallback {
    fun place(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        entity: BlockEntity?,
        source: String,
        player: Player?
    )

    fun place(world: Level, pos: BlockPos, state: BlockState, entity: BlockEntity?, player: Player) =
        place(world, pos, state, entity, Sources.PLAYER, player)

    fun place(world: Level, pos: BlockPos, state: BlockState, entity: BlockEntity?, source: String) =
        place(world, pos, state, entity, source, null)

    companion object {
        @JvmField
        val EVENT: Event<BlockPlaceCallback> =
            EventFactory.createArrayBacked(BlockPlaceCallback::class.java) { listeners ->
                BlockPlaceCallback { world, pos, state, entity, source, player ->
                    for (listener in listeners) {
                        listener.place(world, pos, state, entity, source, player)
                    }
                }
            }
    }
}
