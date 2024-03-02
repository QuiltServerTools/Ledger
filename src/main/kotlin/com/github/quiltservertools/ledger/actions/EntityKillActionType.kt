package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d

class EntityKillActionType : AbstractActionType() {
    override val identifier = "entity-kill"

    override fun getTranslationType() = "entity"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val entityType = Registries.ENTITY_TYPE.getOrEmpty(objectIdentifier)
        if (entityType.isPresent) {
            val entity = entityType.get().create(world)!!
            entity.readNbt(StringNbtReader.parse(extraData))
            entity.velocity = Vec3d.ZERO
            entity.fireTicks = 0
            if (entity is LivingEntity) { entity.health = entity.defaultMaxHealth.toFloat() }

            world?.spawnEntity(entity)

            return true
        }

        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val uuid = StringNbtReader.parse(extraData)!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid)

        if (entity != null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
            return true
        }

        return false
    }
}
