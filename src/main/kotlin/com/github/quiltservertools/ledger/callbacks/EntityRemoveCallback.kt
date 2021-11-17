package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface EntityRemoveCallback {
    fun remove(
        stack: ItemStack,
        world: World,
        pos: BlockPos,
        entity: Entity,
        slot: EquipmentSlot.Type,
        source: String,
        player: ServerPlayerEntity?)

    companion object {
        @JvmField
        val EVENT: Event<EntityRemoveCallback> =
        EventFactory.createArrayBacked(EntityRemoveCallback::class.java) { listeners ->
            EntityRemoveCallback { stack, world, pos, entity, slot, source, player ->
            for (listener in listeners) {
                listener.remove(stack, world, pos, entity, slot, source, player)
            }
        }
        }
    }
}
