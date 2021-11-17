package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityEquipCallback
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.callbacks.EntityRemoveCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World


fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
    EntityEquipCallback.EVENT.register(::onEquip)
    EntityRemoveCallback.EVENT.register(::onRemove)
}

private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
    DatabaseManager.logAction(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

private fun onEquip(playerStack: ItemStack, world: World, pos: BlockPos, entity: LivingEntity, player: PlayerEntity) {
    DatabaseManager.logAction(
        ActionFactory.entityEquipAction(playerStack, world, pos, entity, player)
    )
}

private fun onRemove(entityStack: ItemStack, world: World, pos: BlockPos, entity: LivingEntity, player: PlayerEntity) {
    DatabaseManager.logAction(
        ActionFactory.entityRemoveAction(entityStack, world, pos, entity, player)
    )
}

