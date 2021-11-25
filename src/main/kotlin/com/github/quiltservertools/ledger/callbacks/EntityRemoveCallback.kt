package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface EntityRemoveCallback {
    fun remove(
        stack: ItemStack,
        world: World,
        pos: BlockPos,
        entity: Entity,
        entityActor: Entity
    )

    companion object {
        @JvmField
        val EVENT: Event<EntityRemoveCallback> =
            EventFactory.createArrayBacked(EntityRemoveCallback::class.java) { listeners ->
                EntityRemoveCallback { stack, world, pos, entity, player ->
                    for (listener in listeners) {
                        listener.remove(stack, world, pos, entity, player)
                    }
                }
            }
    }
}
