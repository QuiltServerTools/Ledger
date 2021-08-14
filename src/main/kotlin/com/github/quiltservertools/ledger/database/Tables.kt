package com.github.quiltservertools.ledger.database

import net.minecraft.util.Identifier
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

private const val MAX_PLAYER_NAME_LENGTH = 16
private const val MAX_ACTION_NAME_LENGTH = 16
private const val MAX_IDENTIFIER_LENGTH = 200
private const val MAX_SOURCE_NAME_LENGTH = 30

object Tables {
    object Players : IntIdTable("players") {
        val playerId = uuid("player_id").uniqueIndex()
        val playerName = varchar("player_name", MAX_PLAYER_NAME_LENGTH)
        val firstJoin = timestamp("first_join").clientDefault { Instant.now() }
        val lastJoin = timestamp("last_join").clientDefault { Instant.now() }
    }

    class Player(id: EntityID<Int>) : IntEntity(id) {
        var playerId by Players.playerId
        var playerName by Players.playerName
        var firstJoin by Players.firstJoin
        var lastJoin by Players.lastJoin

        companion object : IntEntityClass<Player>(Players)
    }

    object ActionIdentifiers : IntIdTable() {
        val actionIdentifier = varchar("action_identifier", MAX_ACTION_NAME_LENGTH).uniqueIndex()
    }

    class ActionIdentifier(id: EntityID<Int>) : IntEntity(id) {
        var identifier by ActionIdentifiers.actionIdentifier

        companion object : IntEntityClass<ActionIdentifier>(ActionIdentifiers)
    }

    object ObjectIdentifiers : IntIdTable() {
        val identifier = varchar("identifier", MAX_IDENTIFIER_LENGTH).uniqueIndex()
    }

    class ObjectIdentifier(id: EntityID<Int>) : IntEntity(id) {
        var identifier by ObjectIdentifiers.identifier.transform({ it.toString() }, { Identifier.tryParse(it)!! })

        companion object : IntEntityClass<ObjectIdentifier>(ObjectIdentifiers)
    }

    object Actions : IntIdTable("actions") {
        val actionIdentifier = reference("action_id", ActionIdentifiers.id)
        val timestamp = timestamp("time")
        val x = integer("x").index("pos_x_index")
        val y = integer("y").index("pos_y_index")
        val z = integer("z").index("pos_z_index")
        val world = reference("world_id", Worlds.id).index("world_index", false)
        val objectId = reference("object_id", ObjectIdentifiers.id)
        val oldObjectId = reference("old_object_id", ObjectIdentifiers.id)
        val blockState = text("block_state").nullable()
        val oldBlockState = text("old_block_state").nullable()
        val sourceName = reference("source", Sources.id)
        val sourcePlayer = optReference("player_id", Players.id)
        val extraData = text("extra_data").nullable()
        val rolledBack = bool("rolled_back").clientDefault { false }
    }

    class Action(id: EntityID<Int>) : IntEntity(id) {
        var actionIdentifier by ActionIdentifier referencedOn Actions.actionIdentifier
        var timestamp by Actions.timestamp
        var x by Actions.x
        var y by Actions.y
        var z by Actions.z
        var world by World referencedOn Actions.world
        var objectId by ObjectIdentifier referencedOn Actions.objectId
        var oldObjectId by ObjectIdentifier referencedOn Actions.oldObjectId
        var blockState by Actions.blockState
        var oldBlockState by Actions.oldBlockState
        var sourceName by Source referencedOn Actions.sourceName
        var sourcePlayer by Player optionalReferencedOn Actions.sourcePlayer
        var extraData by Actions.extraData
        var rolledBack by Actions.rolledBack

        companion object : IntEntityClass<Action>(Actions)
    }

    object Sources : IntIdTable("sources") {
        val name = varchar("name", MAX_SOURCE_NAME_LENGTH).uniqueIndex()
    }

    class Source(id: EntityID<Int>) : IntEntity(id) {
        var name by Sources.name

        companion object : IntEntityClass<Source>(Sources)
    }

    object Worlds : IntIdTable("worlds") {
        val identifier = varchar("identifier", MAX_IDENTIFIER_LENGTH).uniqueIndex()
    }

    class World(id: EntityID<Int>) : IntEntity(id) {
        var identifier by Worlds.identifier.transform({ it.toString() }, { Identifier.tryParse(it)!! })

        companion object : IntEntityClass<World>(Worlds)
    }
}
