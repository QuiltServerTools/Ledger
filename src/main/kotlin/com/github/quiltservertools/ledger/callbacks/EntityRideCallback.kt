package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

fun interface EntityRideCallback {
    fun ride(entity: Entity, playerEntity: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<EntityRideCallback> =
            EventFactory.createArrayBacked(EntityRideCallback::class.java) { listeners ->
                EntityRideCallback { entity, player ->
                    for (listener in listeners) {
                        listener.ride(entity, player)
                    }
                }
            }
    }
}
