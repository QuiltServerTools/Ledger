package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.LOGGER
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import net.minecraft.core.UUIDUtil
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.TagParser
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

class EntityKillActionType : AbstractActionType() {
    override val identifier = "entity-kill"

    val noopPacketSender = object : ServerEntity.Synchronizer {
        override fun sendToTrackingPlayers(packet: Packet<in ClientGamePacketListener>) = Unit
        override fun sendToTrackingPlayersAndSelf(packet: Packet<in ClientGamePacketListener>) = Unit
        override fun sendToTrackingPlayersFiltered(
            packet: Packet<in ClientGamePacketListener>,
            predicate: Predicate<ServerPlayer>
        ) = Unit
    }

    override fun getTranslationType() = "entity"

    private fun getEntity(world: ServerLevel, reporter: ProblemReporter): Entity? {
        val entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(objectIdentifier)
        if (entityType.isEmpty) return null

        val entity = entityType.get().create(world, EntitySpawnReason.COMMAND)!!
        val readView = TagValueInput.create(reporter, world.registryAccess(), TagParser.parseCompoundFully(extraData!!))
        entity.load(readView)
        entity.setDeltaMovement(Vec3.ZERO)
        entity.setRemainingFireTicks(0)
        if (entity is LivingEntity) entity.health = entity.maxHealth

        return entity
    }

    override fun previewRollback(preview: Preview, player: ServerPlayer) {
        val world = player.level().server.getWorld(world)!!
        val entity = getEntity(world, ProblemReporter.DISCARDING) ?: return

        val entityTrackerEntry = ServerEntity(world, entity, 1, false, noopPacketSender)
        entityTrackerEntry.addPairing(player)
        preview.spawnedEntityTrackers.add(entityTrackerEntry)
    }

    override fun previewRestore(preview: Preview, player: ServerPlayer) {
        val world = player.level().server.getWorld(world)

        val tag = TagParser.parseCompoundFully(extraData!!)
        val optionalUuid = tag.read("UUID", UUIDUtil.CODEC)
        if (optionalUuid.isPresent) {
            val uuid = optionalUuid.get()
            val entity = world?.getEntity(uuid)
            entity?.let {
                val entityTrackerEntry = ServerEntity(world, entity, 1, false, noopPacketSender)
                entityTrackerEntry.removePairing(player)
                preview.removedEntityTrackers.add(entityTrackerEntry)
            }
        }
    }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)!!
        ProblemReporter.ScopedCollector({ "ledger:rollback:entity-kill@$pos" }, LOGGER).use {
            val entity = getEntity(world, it) ?: return false
            world.addFreshEntity(entity)
        }
        return true
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val optionalUUID = TagParser.parseCompoundFully(extraData!!).read(UUID, UUIDUtil.CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity != null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
            return true
        }

        return false
    }
}
