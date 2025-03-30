package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.EntityTrackerEntry
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Uuids
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

class EntityKillActionType : AbstractActionType() {
    override val identifier = "entity-kill"

    override fun getTranslationType() = "entity"

    private fun getEntity(world: ServerWorld): Entity? {
        val entityType = Registries.ENTITY_TYPE.getOptionalValue(objectIdentifier)
        if (entityType.isEmpty) return null

        val entity = entityType.get().create(world, SpawnReason.COMMAND)!!
        entity.readNbt(StringNbtReader.readCompound(extraData))
        entity.velocity = Vec3d.ZERO
        entity.fireTicks = 0
        if (entity is LivingEntity) entity.health = entity.defaultMaxHealth.toFloat()

        return entity
    }

    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) {
        val world = player.server.getWorld(world)!!
        val entity = getEntity(world)

        val entityTrackerEntry = EntityTrackerEntry(world, entity, 1, false, { }, { _, _ -> })
        entityTrackerEntry.startTracking(player)
        preview.spawnedEntityTrackers.add(entityTrackerEntry)
    }

    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) {
        val world = player.server.getWorld(world)

        val tag = StringNbtReader.readCompound(extraData)
        val optionalUuid = tag.get("UUID", Uuids.INT_STREAM_CODEC)
        if (optionalUuid.isPresent) {
            val uuid = optionalUuid.get()
            val entity = world?.getEntity(uuid)
            entity?.let {
                val entityTrackerEntry = EntityTrackerEntry(world, entity, 1, false, { }, { _, _ -> })
                entityTrackerEntry.stopTracking(player)
                preview.removedEntityTrackers.add(entityTrackerEntry)
            }
        }
    }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)!!
        val entity = getEntity(world) ?: return false

        world.spawnEntity(entity)
        return true
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val optionalUUID = StringNbtReader.readCompound(extraData)!!.get(UUID, Uuids.INT_STREAM_CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity != null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
            return true
        }

        return false
    }
}
