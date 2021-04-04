package us.potatoboy.ledger.listeners

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import us.potatoboy.ledger.actionutils.ActionFactory
import us.potatoboy.ledger.callbacks.BlockExplodeEvent
import us.potatoboy.ledger.database.ActionQueue

object BlockEventListener {
    init {
        BlockExplodeEvent.EVENT.register(::onExplode)
    }

    private fun onExplode(
        world: World,
        entity: Entity?,
        blockPos: BlockPos,
        blockState: BlockState,
        blockEntity: BlockEntity?
    ) {
        val source = entity?.let { Registry.ENTITY_TYPE.getId(it.type).path } ?: "explosion"

        ActionQueue.addActionToQueue(ActionFactory.blockBreakAction(world, blockPos, blockState, source))
    }
}