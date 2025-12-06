package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.BlockBreakCallback
import com.github.quiltservertools.ledger.callbacks.BlockChangeCallback
import com.github.quiltservertools.ledger.callbacks.BlockMeltCallback
import com.github.quiltservertools.ledger.callbacks.BlockPlaceCallback
import com.github.quiltservertools.ledger.database.ActionQueueService
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AirBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

fun registerBlockListeners() {
    BlockMeltCallback.EVENT.register(::onMelt)
    BlockPlaceCallback.EVENT.register(::onBlockPlace)
    BlockBreakCallback.EVENT.register(::onBlockBreak)
    BlockChangeCallback.EVENT.register(::onBlockChange)
}

fun onBlockPlace(
    world: Level,
    pos: BlockPos,
    state: BlockState,
    entity: BlockEntity?,
    source: String,
    player: Player?
) {
    val action: ActionType = if (player != null) {
        ActionFactory.blockPlaceAction(world, pos, state, player, entity, source)
    } else {
        ActionFactory.blockPlaceAction(world, pos, state, source, entity)
    }
    ActionQueueService.addToQueue(action)
}

fun onBlockBreak(
    world: Level,
    pos: BlockPos,
    state: BlockState,
    entity: BlockEntity?,
    source: String,
    player: Player?
) {
    val action: ActionType = if (player != null) {
        ActionFactory.blockBreakAction(world, pos, state, player, entity, source)
    } else {
        ActionFactory.blockBreakAction(world, pos, state, source, entity)
    }

    ActionQueueService.addToQueue(action)
}

fun onBlockChange(
    world: Level,
    pos: BlockPos,
    oldState: BlockState,
    newState: BlockState,
    oldBlockEntity: BlockEntity?,
    newBlockEntity: BlockEntity?,
    source: String,
    player: Player?
) {
    ActionQueueService.addToQueue(
        ActionFactory.blockChangeAction(world, pos, oldState, newState, oldBlockEntity, source, player)
    )
}

fun onMelt(world: Level, pos: BlockPos, oldState: BlockState, newState: BlockState, entity: BlockEntity?) {
    ActionQueueService.addToQueue(
        ActionFactory.blockBreakAction(world, pos, oldState, Sources.MELT, entity)
    )
    if (newState.block !is AirBlock) {
        ActionQueueService.addToQueue(
            ActionFactory.blockPlaceAction(world, pos, newState, Sources.MELT, entity)
        )
    }
}
