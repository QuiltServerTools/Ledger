package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player

fun interface ItemPickUpCallback {
    fun pickUp(entity: ItemEntity, player: Player)

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
