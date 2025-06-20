package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

fun interface EntityDismountCallback {
    fun dismount(entity: Entity, playerEntity: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<EntityDismountCallback> =
            EventFactory.createArrayBacked(EntityDismountCallback::class.java) { listeners ->
                EntityDismountCallback { entity, player ->
                    for (listener in listeners) {
                        listener.dismount(entity, player)
                    }
                }
            }
    }
}
