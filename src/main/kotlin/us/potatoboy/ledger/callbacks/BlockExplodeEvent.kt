package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface BlockExplodeEvent {
    companion object {
        val EVENT: Event<BlockExplodeEvent> =
            EventFactory.createArrayBacked(BlockExplodeEvent::class.java) { listeners ->
                BlockExplodeEvent { world, source, pos, state, entity ->
                    for (listener in listeners) {
                        listener.explode(world, source, pos, state, entity)
                    }
                }
            }
    }

    fun explode(world: World, source: Entity?, pos: BlockPos, state: BlockState, entity: BlockEntity?)
}