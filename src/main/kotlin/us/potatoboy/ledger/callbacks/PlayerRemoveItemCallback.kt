package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

fun interface PlayerRemoveItemCallback {
    fun remove(stack: ItemStack, pos: BlockPos, player: ServerPlayerEntity)

    companion object {
        val EVENT: Event<PlayerRemoveItemCallback> =
            EventFactory.createArrayBacked(PlayerRemoveItemCallback::class.java) { listeners ->
                PlayerRemoveItemCallback { stack, pos, player ->
                    for (listener in listeners) {
                        listener.remove(stack, pos, player)
                    }
                }
            }
    }
}
