package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockExplodeCallback {
    fun explode(world: World, source: Entity?, pos: BlockPos, state: BlockState)

    companion object {
        val EVENT: Event<BlockExplodeCallback> =
            EventFactory.createArrayBacked(BlockExplodeCallback::class.java) { listeners ->
                BlockExplodeCallback { world, source, pos, state ->
                    for (listener in listeners) {
                        listener.explode(world, source, pos, state)
                    }
                }
            }
    }
}
