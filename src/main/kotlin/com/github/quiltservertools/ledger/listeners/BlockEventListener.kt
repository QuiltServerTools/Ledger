package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.BlockBurnCallback
import com.github.quiltservertools.ledger.callbacks.BlockDecayCallback
import com.github.quiltservertools.ledger.callbacks.BlockExplodeCallback
import com.github.quiltservertools.ledger.callbacks.BlockFallCallback
import com.github.quiltservertools.ledger.callbacks.BlockLandCallback
import com.github.quiltservertools.ledger.callbacks.BlockMeltCallback
import com.github.quiltservertools.ledger.callbacks.WorldBlockBreakCallback
import com.github.quiltservertools.ledger.callbacks.WorldBlockPlaceCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.block.AirBlock
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.TntEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World


fun registerBlockListeners() {
    BlockExplodeCallback.EVENT.register(::onExplode)
    BlockBurnCallback.EVENT.register(::onBurn)
    BlockFallCallback.EVENT.register(::onFall)
    BlockLandCallback.EVENT.register(::onLand)
    BlockDecayCallback.EVENT.register(::onDecay)
    BlockMeltCallback.EVENT.register(::onMelt)
    WorldBlockPlaceCallback.EVENT.register(::onWorldPlace)
    WorldBlockBreakCallback.EVENT.register(::onWorldBreak)
}

fun onWorldPlace(world: World, pos: BlockPos, state: BlockState, source: String, player: PlayerEntity?) {
    val action = ActionFactory.blockPlaceAction(world, pos, state, source, null)
    if (player != null) {
        action.sourceProfile = player.gameProfile
    }
    DatabaseManager.logAction(action)
}

fun onWorldBreak(world: World, pos: BlockPos, state: BlockState, source: String, player: PlayerEntity?) {
    val action = ActionFactory.blockBreakAction(world, pos, state, source, null)
    if (player != null) {
        action.sourceProfile = player.gameProfile
    }
    DatabaseManager.logAction(action)
}

private fun onLand(world: World, pos: BlockPos, state: BlockState) {
    DatabaseManager.logAction(
        ActionFactory.blockPlaceAction(world, pos, state, Sources.GRAVITY)
    )
}

private fun onFall(world: World, pos: BlockPos, state: BlockState) {
    DatabaseManager.logAction(
        ActionFactory.blockBreakAction(
            world,
            pos,
            state,
            Sources.GRAVITY
        )
    )
}

private fun onBurn(world: World, pos: BlockPos, state: BlockState, entity: BlockEntity?) {
    DatabaseManager.logAction(
        ActionFactory.blockBreakAction(
            world,
            pos,
            state,
            Sources.FIRE,
            entity
        )
    )
}

private fun onExplode(
    world: World,
    source: Entity?,
    blockPos: BlockPos,
    blockState: BlockState,
    entity: BlockEntity?
) {
    val sourceName = source?.let { Registry.ENTITY_TYPE.getId(it.type).path } ?: Sources.EXPLOSION

    val action = ActionFactory.blockBreakAction(
        world,
        blockPos,
        blockState,
        sourceName,
        entity
    )

    if (source is TntEntity) {
        getTntSource(source, action)
    }

    DatabaseManager.logAction(action)
}

fun onDecay(world: World, pos: BlockPos, state: BlockState, entity: BlockEntity?) {
    DatabaseManager.logAction(
        ActionFactory.blockBreakAction(world, pos, state, Sources.DECAY, entity)
    )
}

fun onMelt(world: World, pos: BlockPos, oldState: BlockState, newState: BlockState, entity: BlockEntity?) {
    DatabaseManager.logAction(
        ActionFactory.blockBreakAction(world, pos, oldState, Sources.MELT, entity)
    )
    if (newState.block !is AirBlock) {
        DatabaseManager.logAction(
            ActionFactory.blockPlaceAction(world, pos, newState, Sources.MELT, entity)
        )
    }
}

private fun getTntSource(entity: TntEntity, action: ActionType) {
    if (entity.causingEntity is TntEntity) {
        getTntSource(entity, action)
    }
    if (entity.causingEntity is PlayerEntity) {
        action.sourceProfile = (entity.causingEntity as PlayerEntity).gameProfile
    }
}
