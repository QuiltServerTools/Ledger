package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

fun interface ItemRemoveCallback {
    fun remove(stack: ItemStack, pos: BlockPos, world: ServerLevel, source: String, entity: LivingEntity?)

    companion object {
        @JvmField
        val EVENT: Event<ItemRemoveCallback> =
            EventFactory.createArrayBacked(ItemRemoveCallback::class.java) { listeners ->
                ItemRemoveCallback { stack, pos, world, source, player ->
                    for (listener in listeners) {
                        listener.remove(stack, pos, world, source, player)
                    }
                }
            }
    }
}
