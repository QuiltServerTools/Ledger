package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

fun interface EntityMountCallback {
    fun mount(entity: Entity, playerEntity: Player)

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
