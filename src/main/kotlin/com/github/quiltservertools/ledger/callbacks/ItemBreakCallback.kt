package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

fun interface ItemBreakCallback {
    fun breakItem(stack: ItemStack, player: Player)

    companion object {
        @JvmField
        val EVENT: Event<ItemBreakCallback> = EventFactory.createArrayBacked(
            ItemBreakCallback::class.java,
        ) { listeners ->
            ItemBreakCallback { stack, player ->
                for (listener in listeners) {
                    listener.breakItem(stack, player)
                }
            }
        }
    }
}
