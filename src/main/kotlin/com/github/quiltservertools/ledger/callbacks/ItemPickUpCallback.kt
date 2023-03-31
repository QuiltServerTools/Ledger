package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity

fun interface ItemPickUpCallback {
    fun pickUp(entity: ItemEntity, player: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<ItemPickUpCallback> =
            EventFactory.createArrayBacked(ItemPickUpCallback::class.java) { listeners ->
                ItemPickUpCallback { entity, player ->
                    for (listener in listeners) {
                        listener.pickUp(entity, player)
                    }
                }
            }
    }
}
