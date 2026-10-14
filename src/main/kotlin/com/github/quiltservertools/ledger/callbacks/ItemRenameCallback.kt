package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

fun interface ItemRenameCallback {
    fun rename(player: Player, pos: BlockPos, oldStack: ItemStack, newStack: ItemStack)

    companion object {
        @JvmField
        val EVENT: Event<ItemRenameCallback> = EventFactory.createArrayBacked(
            ItemRenameCallback::class.java,
        ) { listeners ->
            ItemRenameCallback { player, pos, oldStack, newStack ->
                for (listener in listeners) {
                    listener.rename(player, pos, oldStack, newStack)
                }
            }
        }
    }
}
