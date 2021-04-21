package us.potatoboy.ledger.listeners

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import us.potatoboy.ledger.actionutils.ActionFactory
import us.potatoboy.ledger.callbacks.EntityKillCallback
import us.potatoboy.ledger.database.DatabaseQueue
import us.potatoboy.ledger.database.queueitems.ActionQueueItem

object EntityCallbackListener {
    init {
        EntityKillCallback.EVENT.register(::onKill)
    }

    private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
        DatabaseQueue.addActionToQueue(ActionQueueItem(ActionFactory.entityKillAction(world, pos, entity, source)))
    }
}