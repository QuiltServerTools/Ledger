package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity

fun interface ItemDropCallback {
    fun drop(entity: ItemEntity, player: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<ItemDropCallback> = EventFactory.createArrayBacked(ItemDropCallback::class.java) { listeners ->
            ItemDropCallback { entity, player ->
                for (listener in listeners) {
                    listener.drop(entity, player)
                }
            }
        }
    }
}
