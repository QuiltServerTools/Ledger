package com.github.quiltservertools.ledger.callbacks

import com.github.quiltservertools.ledger.utility.Sources
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

fun interface BlockChangeCallback {
    fun changeBlock(
        world: Level,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?,
        source: String,
        player: Player?
    )

    fun changeBlock(
        world: Level,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?,
        player: Player
    ) =
        changeBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, Sources.PLAYER, player)

    fun changeBlock(
        world: Level,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?,
        source: String
    ) =
        changeBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, null)

    companion object {
        @JvmField
        val EVENT: Event<BlockChangeCallback> =
            EventFactory.createArrayBacked(BlockChangeCallback::class.java) { listeners ->
                BlockChangeCallback { world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, player ->
                    for (listener in listeners) {
                        listener.changeBlock(
                            world,
                            pos,
                            oldState,
                            newState,
                            oldBlockEntity,
                            newBlockEntity,
                            source,
                            player
                        )
                    }
                }
            }
    }
}
