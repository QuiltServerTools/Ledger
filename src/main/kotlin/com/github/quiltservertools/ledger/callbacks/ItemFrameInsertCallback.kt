package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

fun interface ItemFrameInsertCallback {
    fun insert(
        stack: ItemStack,
        pos: BlockPos,
        world: ServerWorld,
        source: String,
        player: ServerPlayerEntity?,
        itemFrameEntity: ItemFrameEntity
    )

    companion object {
        @JvmField
        val EVENT: Event<ItemFrameInsertCallback> =
            EventFactory.createArrayBacked(ItemFrameInsertCallback::class.java) { listeners ->
                ItemFrameInsertCallback { stack, pos, world, source, player, itemFrameEntity ->
                    for (listener in listeners) {
                        listener.insert(stack, pos, world, source, player, itemFrameEntity)
                    }
                }
            }
    }
}
