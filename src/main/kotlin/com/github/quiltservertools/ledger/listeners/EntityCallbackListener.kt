package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.callbacks.EntityModifyCallback
import com.github.quiltservertools.ledger.database.ActionQueueService
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
    EntityModifyCallback.EVENT.register(::onModify)
}

private fun onKill(
    world: World,
    pos: BlockPos,
    entity: Entity,
    source: DamageSource
) {
    ActionQueueService.addToQueue(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

private fun onModify(
    world: World,
    pos: BlockPos,
    oldEntityTags: NbtCompound,
    entity: Entity,
    itemStack: ItemStack?,
    entityActor: Entity?,
    sourceType: String
) {
    ActionQueueService.addToQueue(
        ActionFactory.entityChangeAction(world, pos, oldEntityTags, entity, itemStack, entityActor, sourceType)
    )
}
