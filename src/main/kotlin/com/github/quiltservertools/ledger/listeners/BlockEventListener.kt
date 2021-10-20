package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.BlockBurnCallback
import com.github.quiltservertools.ledger.callbacks.BlockDecayCallback
import com.github.quiltservertools.ledger.callbacks.BlockExplodeCallback
import com.github.quiltservertools.ledger.callbacks.BlockFallCallback
import com.github.quiltservertools.ledger.callbacks.BlockLandCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Sources
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

private fun getTntSource(entity: TntEntity, action: ActionType) {
    if (entity.causingEntity is TntEntity) {
        getTntSource(entity, action)
    }
    if (entity.causingEntity is PlayerEntity) {
        action.sourceProfile = (entity.causingEntity as PlayerEntity).gameProfile
    }
}
