package us.potatoboy.ledger.listeners

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import us.potatoboy.ledger.actionutils.ActionFactory
import us.potatoboy.ledger.callbacks.EntityKillCallback
import us.potatoboy.ledger.database.ActionQueue

object EntityCallbackListener {
    init {
        EntityKillCallback.EVENT.register(::onKill)
    }

    private fun onKill(world: World, pos: BlockPos, entity: LivingEntity, source: DamageSource) {
        ActionQueue.addActionToQueue(ActionFactory.entityKillAction(world, pos, entity, source))
    }
}