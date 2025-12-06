package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

fun interface EntityDismountCallback {
    fun dismount(entity: Entity, playerEntity: Player)

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
