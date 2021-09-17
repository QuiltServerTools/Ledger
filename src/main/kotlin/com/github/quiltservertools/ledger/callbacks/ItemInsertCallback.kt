package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

fun interface ItemInsertCallback {
    fun insert(stack: ItemStack, pos: BlockPos, world: ServerWorld, source: String, player: ServerPlayerEntity?)

    companion object {
        @JvmField
        val EVENT: Event<ItemInsertCallback> =
            EventFactory.createArrayBacked(ItemInsertCallback::class.java) { listeners ->
                ItemInsertCallback { stack, pos, world, source, player ->
                    for (listener in listeners) {
                        listener.insert(stack, pos, world, source, player)
                    }
                }
            }
    }
}
