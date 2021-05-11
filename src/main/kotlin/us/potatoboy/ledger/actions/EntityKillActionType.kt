package us.potatoboy.ledger.actions

import net.minecraft.entity.LivingEntity
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d
import net.minecraft.util.registry.Registry

class EntityKillActionType : AbstractActionType() {
    override val identifier = "entity-kill"

    override fun getTranslationType() = "entity"

    override fun rollback(world: ServerWorld): Boolean {
        val entityType = Registry.ENTITY_TYPE.getOrEmpty(objectIdentifier)
        if (entityType.isPresent) {
            val entity: LivingEntity = (entityType.get().create(world) as LivingEntity?)!!
            entity.fromTag(StringNbtReader.parse(extraData))
            entity.health = entity.defaultMaxHealth.toFloat()
            entity.velocity = Vec3d.ZERO
            entity.fireTicks = 0

            world.spawnEntity(entity)

            return true
        }

        return false
    }

    override fun restore(world: ServerWorld): Boolean {
        val tag = StringNbtReader.parse(extraData)
        if (tag.containsUuid("UUID")) {
            val uuid = tag.getUuid("UUID")
            val entity = world.getEntity(uuid)

            if (entity != null) {
                entity.remove()
                return true
            }
        }

        return false
    }
}
