package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.AbstractDecorationEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface DecorationEntityKillCallback {

    fun kill(world: World, pos: BlockPos, entity: AbstractDecorationEntity, source: Entity?)

    companion object {
        @JvmField
        val EVENT: Event<DecorationEntityKillCallback> =
            EventFactory.createArrayBacked(DecorationEntityKillCallback::class.java) { listeners ->
                DecorationEntityKillCallback { world, pos, entity, source ->
                    for (listener in listeners) {
                        listener.kill(world, pos, entity, source)
                    }
                }
            }
    }
}
