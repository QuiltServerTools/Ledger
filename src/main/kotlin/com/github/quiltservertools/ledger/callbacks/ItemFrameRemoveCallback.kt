package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

fun interface ItemFrameRemoveCallback {
    fun remove(
        stack: ItemStack,
        pos: BlockPos,
        world: ServerWorld,
        source: String,
        player: ServerPlayerEntity?,
        itemFrameEntity: ItemFrameEntity
    )

    companion object {
        @JvmField
        val EVENT: Event<ItemFrameRemoveCallback> =
            EventFactory.createArrayBacked(ItemFrameRemoveCallback::class.java) { listeners ->
                ItemFrameRemoveCallback { stack, pos, world, source, player, itemFrameEntity ->
                    for (listener in listeners) {
                        listener.remove(stack, pos, world, source, player, itemFrameEntity)
                    }
                }
            }
    }
}
