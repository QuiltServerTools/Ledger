package com.github.quiltservertools.ledger.callbacks

import com.github.quiltservertools.ledger.utility.Sources
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockChangeCallback {
    fun changeBlock(
        world: World,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?,
        source: String,
        player: PlayerEntity?
    )

    fun changeBlock(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, oldBlockEntity: BlockEntity?, newBlockEntity: BlockEntity?, player: PlayerEntity) =
        changeBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, Sources.PLAYER, player)

    fun changeBlock(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, oldBlockEntity: BlockEntity?, newBlockEntity: BlockEntity?, source: String) =
        changeBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, null)

    companion object {
        @JvmField
        val EVENT: Event<BlockChangeCallback> =
            EventFactory.createArrayBacked(BlockChangeCallback::class.java) { listeners ->
                BlockChangeCallback { world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, player ->
                    for (listener in listeners) {
                        listener.changeBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, player)
                    }
                }
            }
    }
}
