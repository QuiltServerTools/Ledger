package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback
import com.github.quiltservertools.ledger.database.ActionQueueService
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
    EntityModifyCallback.EVENT.register(::onModify)
}

private fun onKill(
    world: Level,
    pos: BlockPos,
    entity: Entity,
    source: DamageSource
) {
    ActionQueueService.addToQueue(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

fun onKill(
    world: Level,
    pos: BlockPos,
    entity: Entity,
    source: String
) {
    ActionQueueService.addToQueue(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

private fun onModify(
    world: Level,
    pos: BlockPos,
    oldEntityTags: CompoundTag,
    entity: Entity,
    itemStack: ItemStack?,
    entityActor: Entity?,
    sourceType: String
) {
    ActionQueueService.addToQueue(
        ActionFactory.entityChangeAction(world, pos, oldEntityTags, entity, itemStack, entityActor, sourceType)
    )
}
