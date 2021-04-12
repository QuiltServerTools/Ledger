package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockBurnEvent {
    companion object {
        val EVENT: Event<BlockBurnEvent> =
            EventFactory.createArrayBacked(BlockBurnEvent::class.java) { listeners ->
                BlockBurnEvent { world, pos, state ->
                    for (listener in listeners) {
                        listener.burn(world, pos, state)
                    }
                }
            }
    }

    fun burn(world: World, pos: BlockPos, state: BlockState)
}