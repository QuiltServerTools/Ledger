package us.potatoboy.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun interface PlayerBlockPlaceCallback {
    fun place(
        world: World,
        placer: LivingEntity,
        pos: BlockPos,
        state: BlockState,
        entity: BlockEntity?
    )

    companion object {
        val EVENT: Event<PlayerBlockPlaceCallback> =
            EventFactory.createArrayBacked(PlayerBlockPlaceCallback::class.java) { listeners ->
                PlayerBlockPlaceCallback { world, placer, pos, state, entity ->
                    for (listener in listeners) {
                        listener.place(world, placer, pos, state, entity)
                    }
                }
            }
    }
}
