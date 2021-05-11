package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

fun interface PlayerInsertItemCallback {
    fun insert(stack: ItemStack, pos: BlockPos, player: ServerPlayerEntity)

    companion object {
        val EVENT: Event<PlayerInsertItemCallback> =
            EventFactory.createArrayBacked(PlayerInsertItemCallback::class.java) { listeners ->
                PlayerInsertItemCallback { stack, pos, player ->
                    for (listener in listeners) {
                        listener.insert(stack, pos, player)
                    }
                }
            }
    }
}
