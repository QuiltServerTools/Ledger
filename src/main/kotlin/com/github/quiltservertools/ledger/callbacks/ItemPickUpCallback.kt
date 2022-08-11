package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface ItemPickUpCallback {
    fun pickUp(world: World, pos: BlockPos, stack: ItemStack, player: PlayerEntity)

    companion object {
        @JvmField
        val EVENT: Event<ItemPickUpCallback> =
            EventFactory.createArrayBacked(ItemPickUpCallback::class.java) { listeners ->
                ItemPickUpCallback { world, pos, stack, player ->
                    for (listener in listeners) {
                        listener.pickUp(world, pos, stack, player)
                    }
                }
            }
    }
}
