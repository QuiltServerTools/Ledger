package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity

fun interface EntityMountCallback {
    fun mount(entity: Entity, playerEntity: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<EntityMountCallback> =
            EventFactory.createArrayBacked(EntityMountCallback::class.java) { listeners ->
                EntityMountCallback { entity, player ->
                    for (listener in listeners) {
                        listener.mount(entity, player)
                    }
                }
            }
    }
}
