package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.database.ActionQueueService
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
}

private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
    ActionQueueService.addToQueue(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

