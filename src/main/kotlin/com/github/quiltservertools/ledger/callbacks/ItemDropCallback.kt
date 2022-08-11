package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface ItemDropCallback {
    fun drop(world: World, pos: BlockPos, stack: ItemStack, player: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<ItemDropCallback> = EventFactory.createArrayBacked(ItemDropCallback::class.java) { listeners ->
            ItemDropCallback { world, pos, stack, player ->
                for (listener in listeners) {
                    listener.drop(world, pos, stack, player)
                }
            }
        }
    }
}
