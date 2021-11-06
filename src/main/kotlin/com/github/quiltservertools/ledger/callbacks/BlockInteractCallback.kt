package com.github.quiltservertools.ledger.callbacks

import com.github.quiltservertools.ledger.utility.Sources
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockInteractCallback {
    fun interactBlock(
        world: World,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?,
        source: String,
        player: PlayerEntity?
    )

    fun interactBlock(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, oldBlockEntity: BlockEntity?, newBlockEntity: BlockEntity?, player: PlayerEntity) =
        interactBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, Sources.PLAYER, player)

    fun interactBlock(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, oldBlockEntity: BlockEntity?, newBlockEntity: BlockEntity?, source: String) =
        interactBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, null)

    companion object {
        @JvmField
        val EVENT: Event<BlockInteractCallback> =
            EventFactory.createArrayBacked(BlockInteractCallback::class.java) { listeners ->
                BlockInteractCallback { world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, player ->
                    for (listener in listeners) {
                        listener.interactBlock(world, pos, oldState, newState, oldBlockEntity, newBlockEntity, source, player)
                    }
                }
            }
    }
}
