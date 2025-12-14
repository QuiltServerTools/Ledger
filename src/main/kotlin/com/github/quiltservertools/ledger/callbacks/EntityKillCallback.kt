package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

fun interface EntityKillCallback {
    fun kill(world: Level, pos: BlockPos, entity: Entity, source: DamageSource)

    companion object {
        @JvmField
        val EVENT: Event<EntityKillCallback> =
            EventFactory.createArrayBacked(EntityKillCallback::class.java) { listeners ->
                EntityKillCallback { world, pos, entity, source ->
                    for (listener in listeners) {
                        listener.kill(world, pos, entity, source)
                    }
                }
            }
    }
}
