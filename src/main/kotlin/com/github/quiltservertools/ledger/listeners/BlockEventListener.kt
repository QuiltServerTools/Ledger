package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback
import com.github.quiltservertools.ledger.callbacks.BlockMeltCallback
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback
import com.github.quiltservertools.ledger.database.ActionQueueService
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.block.AirBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


fun registerBlockListeners() {
    BlockMeltCallback.EVENT.register(::onMelt)
    BlockPlaceCallback.EVENT.register(::onBlockPlace)
    BlockBreakCallback.EVENT.register(::onBlockBreak)
    BlockChangeCallback.EVENT.register(::onBlockChange)
}

fun onBlockPlace(
    world: World,
    pos: BlockPos,
    state: BlockState,
    entity: BlockEntity?,
    source: String,
    player: PlayerEntity?
) {
    val action: ActionType = if (player != null) {
        ActionFactory.blockPlaceAction(world, pos, state, player, entity, source)
    } else {
        ActionFactory.blockPlaceAction(world, pos, state, source, entity)
    }
    ActionQueueService.addToQueue(action)
}

fun onBlockBreak(
    world: World,
    pos: BlockPos,
    state: BlockState,
    entity: BlockEntity?,
    source: String,
    player: PlayerEntity?
) {
    val action: ActionType = if (player != null) {
        ActionFactory.blockBreakAction(world, pos, state, player, entity, source)
    } else {
        ActionFactory.blockBreakAction(world, pos, state, source, entity)
    }

    ActionQueueService.addToQueue(action)
}

fun onBlockChange(
    world: World,
    pos: BlockPos,
    oldState: BlockState,
    newState: BlockState,
    oldBlockEntity: BlockEntity?,
    newBlockEntity: BlockEntity?,
    source: String,
    player: PlayerEntity?
) {
    ActionQueueService.addToQueue(
        ActionFactory.blockChangeAction(world, pos, oldState, newState, oldBlockEntity, source, player)
    )
}

fun onMelt(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, entity: BlockEntity?) {
    ActionQueueService.addToQueue(
        ActionFactory.blockBreakAction(world, pos, oldState, Sources.MELT, entity)
    )
    if (newState.block !is AirBlock) {
        ActionQueueService.addToQueue(
            ActionFactory.blockPlaceAction(world, pos, newState, Sources.MELT, entity)
        )
    }
}
