package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.item.ItemEntity

fun interface ItemDespawnCallback {
    fun despawn(entity: ItemEntity)

    companion object {
        @JvmField
        val EVENT: Event<ItemDespawnCallback> = EventFactory.createArrayBacked(
            ItemDespawnCallback::class.java,
        ) { listeners ->
            ItemDespawnCallback { entity ->
                for (listener in listeners) {
                    listener.despawn(entity)
                }
            }
        }
    }
}
