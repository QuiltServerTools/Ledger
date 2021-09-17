package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface EntityKillCallback {
    fun kill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource)

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
