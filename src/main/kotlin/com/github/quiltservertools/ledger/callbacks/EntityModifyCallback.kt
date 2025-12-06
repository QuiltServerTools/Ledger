package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

fun interface EntityModifyCallback {
    fun modify(
        world: Level,
        pos: BlockPos,
        oldEntityTags: CompoundTag,
        newEntity: Entity,
        itemStack: ItemStack?,
        entityActor: Entity?,
        sourceType: String
    )

    companion object {
        @JvmField
        val EVENT: Event<EntityModifyCallback> =
            EventFactory.createArrayBacked(EntityModifyCallback::class.java) { listeners ->
                EntityModifyCallback { world, pos, oldEntityTags, newEntity, itemStack, entityActor, sourceType ->
                    for (listener in listeners) {
                        listener.modify(world, pos, oldEntityTags, newEntity, itemStack, entityActor, sourceType)
                    }
                }
            }
    }
}
