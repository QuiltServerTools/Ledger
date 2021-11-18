package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface EntityEquipCallback {
    fun equip(
        stack: ItemStack,
        world: World,
        pos: BlockPos,
        entity: Entity,
        player: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<EntityEquipCallback> =
        EventFactory.createArrayBacked(EntityEquipCallback::class.java) { listeners ->
            EntityEquipCallback { stack, world, pos, entity, player ->
            for (listener in listeners) {
                listener.equip(stack, world, pos, entity, player)
            }
        }
        }
    }
}
