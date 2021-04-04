package us.potatoboy.ledger.database

import net.minecraft.util.Identifier
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import us.potatoboy.ledger.database.Tables.ObjectIdentifier.Companion.transform
import java.time.Instant

object Tables {
    object Players : IntIdTable("players") {
        val playerId = uuid("player_id").uniqueIndex()
        val playerName = varchar("player_name", 16)
        val firstJoin = timestamp("first_join").clientDefault { Instant.now() }
        val lastJoin = timestamp("last_join").clientDefault { Instant.now() }
    }

    class Player(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Player>(Players)

        var playerId by Players.playerId
        var playerName by Players.playerName
        var firstJoin by Players.firstJoin
        var lastJoin by Players.lastJoin
    }

    object ActionIdentifiers : IntIdTable() {
        val actionIdentifier = varchar("action_identifier", 16).uniqueIndex()
    }

    class ActionIdentifier(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<ActionIdentifier>(ActionIdentifiers)

        var actionIdentifier by ActionIdentifiers.actionIdentifier
    }

    object ObjectIdentifiers : IntIdTable() {
        val identifier = varchar("identifier", 200).uniqueIndex()
    }

    class ObjectIdentifier(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<ObjectIdentifier>(ObjectIdentifiers)

        var identifier by ObjectIdentifiers.identifier.transform({ it.toString() }, { Identifier.tryParse(it)!! })
    }

    object Actions : IntIdTable("actions") {
        val actionIdentifier = reference("action_id", ActionIdentifiers.id)
        val timestamp = timestamp("time")
        val x = integer("x")
        val y = integer("y")
        val z = integer("z")
        val world = reference("world_id", Worlds.id)
        val objectId = reference("object_id", ObjectIdentifiers.id)
        val blockState = text("block_state").nullable()
        val sourceName = reference("source", Sources.id)
        val sourcePlayer = optReference("player_id", Players.id)
        val extraData = text("extra_data").nullable()
        val rolledBack = bool("rolled_back").clientDefault { false }
    }

    class Action(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Action>(Actions)

        var actionIdentifier by ActionIdentifier referencedOn Actions.actionIdentifier
        var timestamp by Actions.timestamp
        var x by Actions.x
        var y by Actions.y
        var z by Actions.z
        var world by World referencedOn Actions.world
        var objectId by ObjectIdentifier referencedOn Actions.objectId
        var blockState by Actions.blockState
        var sourceName by Source referencedOn Actions.sourceName
        var sourcePlayer by Player optionalReferencedOn Actions.sourcePlayer
        var extraData by Actions.extraData
        var rolledBack by Actions.rolledBack
    }

    object Sources : IntIdTable("sources") {
        val name = varchar("name", 20).uniqueIndex()
    }

    class Source(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Source>(Sources)

        var name by Sources.name
    }

    object Worlds : IntIdTable("worlds") {
        val identifier = varchar("identifier", 200).uniqueIndex()
    }

    class World(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<World>(Worlds)

        var identifier by Worlds.identifier.transform({ it.toString() }, { Identifier.tryParse(it)!! })
    }
}