package com.github.quiltservertools.ledger.listeners

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityKillCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity


fun registerEntityListeners() {
    EntityKillCallback.EVENT.register(::onKill)
    EntityKillCallback.EVENT.register(::onEquip)
    EntityKillCallback.EVENT.register(::onRemove)
}

private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
    DatabaseManager.logAction(
        ActionFactory.entityKillAction(world, pos, entity, source)
    )
}

private fun onEquip(playerStack: ItemStack, world: World, pos: BlockPos, entity: Entity, slot: EquipmentSlot.Type, source: String, player: ServerPlayerEntity?) {
    DatabaseManager.logAction(
        ActionFactory.entityEquipAction(playerStack, world, pos, entity, slot, source, player)
    )
}

private fun onRemove(entityStack: ItemStack, world: World, pos: BlockPos, entity: Entity, slot: EquipmentSlot.Type, source: String, player: ServerPlayerEntity?) {
    DatabaseManager.logAction(
        ActionFactory.entityRemoveAction(entityStack, world, pos, entity, slot, source, player)
    )
}

