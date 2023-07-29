package com.github.quiltservertools.ledger.listeners

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.DecorationEntityKillCallback
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.entityKillAction
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.AbstractDecorationEntity


fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
    DecorationEntityKillCallback.EVENT.register(::onKill)
}

private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
    DatabaseManager.logAction(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

private fun onKill(world: World, pos: BlockPos, entity: AbstractDecorationEntity, source: Entity?) {
    DatabaseManager.logAction(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}
