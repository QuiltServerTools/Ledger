package com.github.quiltservertools.ledger.callbacks

import com.github.quiltservertools.ledger.utility.Sources
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

fun interface BlockBreakCallback {
    fun breakBlock(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        entity: BlockEntity?,
        source: String,
        player: Player?
    )

    fun breakBlock(world: Level, pos: BlockPos, state: BlockState, entity: BlockEntity?, player: Player) =
        breakBlock(world, pos, state, entity, Sources.PLAYER, player)

    fun breakBlock(world: Level, pos: BlockPos, state: BlockState, entity: BlockEntity?, source: String) =
        breakBlock(world, pos, state, entity, source, null)

    companion object {
        @JvmField
        val EVENT: Event<BlockBreakCallback> =
            EventFactory.createArrayBacked(BlockBreakCallback::class.java) { listeners ->
                BlockBreakCallback { world, pos, state, entity, source, player ->
                    for (listener in listeners) {
                        listener.breakBlock(world, pos, state, entity, source, player)
                    }
                }
            }
    }
}
