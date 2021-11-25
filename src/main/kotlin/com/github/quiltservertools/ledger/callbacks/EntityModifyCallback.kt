package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface EntityModifyCallback {
    fun modify(
        sourceType: String,
        world: World,
        pos: BlockPos,
        entity: Entity,
        itemStack: ItemStack?,
        entityActor: Entity?)

    companion object {
        @JvmField
        val EVENT: Event<EntityModifyCallback> =
            EventFactory.createArrayBacked(EntityModifyCallback::class.java) { listeners ->
                EntityModifyCallback {actionType, world, pos, entity, itemStack, entityActor ->
                    for (listener in listeners) {
                        listener.modify(actionType, world, pos, entity, itemStack, entityActor)
                    }
                }
            }
    }
}
