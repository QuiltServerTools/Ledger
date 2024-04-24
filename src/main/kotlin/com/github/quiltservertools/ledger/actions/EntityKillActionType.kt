package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.EntityTrackerEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.Vec3d

class EntityKillActionType : AbstractActionType() {
    override val identifier = "entity-kill"

    override fun getTranslationType() = "entity"

    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) {
        val world = player.server.getWorld(world)

        val entityType = Registries.ENTITY_TYPE.getOrEmpty(objectIdentifier)
        if (entityType.isEmpty) return

        val entity: LivingEntity = (entityType.get().create(world) as LivingEntity?)!!
        entity.readNbt(StringNbtReader.parse(extraData))
        entity.health = entity.defaultMaxHealth.toFloat()
        entity.velocity = Vec3d.ZERO
        entity.fireTicks = 0
        val entityTrackerEntry = EntityTrackerEntry(world, entity, 1, false) { }
        entityTrackerEntry.startTracking(player)
        preview.spawnedEntityTrackers.add(entityTrackerEntry)
    }

    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) {
        val world = player.server.getWorld(world)

        val tag = StringNbtReader.parse(extraData)
        if (tag.containsUuid("UUID")) {
            val uuid = tag.getUuid("UUID")
            val entity = world?.getEntity(uuid)
            entity?.let {
                val entityTrackerEntry = EntityTrackerEntry(world, entity, 1, false) { }
                entityTrackerEntry.stopTracking(player)
                preview.removedEntityTrackers.add(entityTrackerEntry)
            }
        }
    }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val entityType = Registries.ENTITY_TYPE.getOrEmpty(objectIdentifier)
        if (entityType.isPresent) {
            val entity = entityType.get().create(world)!!
            entity.readNbt(StringNbtReader.parse(extraData))
            entity.velocity = Vec3d.ZERO
            entity.fireTicks = 0
            if (entity is LivingEntity) entity.health = entity.defaultMaxHealth.toFloat()

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
