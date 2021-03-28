package us.potatoboy.ledger.database

import net.minecraft.util.Identifier
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import us.potatoboy.ledger.Ledger
import java.time.Instant

object Tables {
    object Players : IntIdTable("players") {
        val player_id = uuid("player_id").uniqueIndex()
        val player_name = varchar("player_name", 16)
        val first_join = timestamp("first_join").clientDefault { Instant.now() }
        val last_join = timestamp("last_join").clientDefault { Instant.now() }
    }

    class Player(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Player>(Players)

        var playerId by Players.player_id
        var playerName by Players.player_name
        var firstJoin by Players.first_join
        var lastJoin by Players.last_join
    }

    object ActionIdentifiers : IntIdTable() {
        val action_identifier = varchar("action_identifier", 16).uniqueIndex()
    }

    class ActionIdentifier(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<ActionIdentifier>(ActionIdentifiers)

        var actionIdentifier by ActionIdentifiers.action_identifier
    }

    object RegistryKeys : IntIdTable() {
        val registry_key = varchar("registry_key", 200).uniqueIndex()
    }

    class RegistryKey(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<RegistryKey>(RegistryKeys)

        var identifier by RegistryKeys.registry_key.transform({ it.toString() }, { Identifier.tryParse(it)!! })
    }

    object Actions : IntIdTable("actions") {
        val actionIdentifier = reference("action_id", ActionIdentifiers)
        val timestamp = timestamp("time")
        val x = integer("x")
        val y = integer("y")
        val z = integer("z")
        val world = reference("world_id", RegistryKeys)
        val object_id = reference("object_id", RegistryKeys)
        val block_state = text("block_state").nullable()
        val source_name = varchar("source_name", 20)
        val source_player = reference("player_id", Players).nullable()
        val extra_data = text("extra_data").nullable()
    }

    class Action(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Action>(Actions)

        var actionIdentifier by ActionIdentifier referencedOn Actions.actionIdentifier
        var timestamp by Actions.timestamp
        var x by Actions.x
        var y by Actions.y
        var z by Actions.z
        var world by RegistryKey referencedOn Actions.world
        var objectId by RegistryKey referencedOn Actions.object_id
        var blockState by Actions.block_state
        var sourceName by Actions.source_name
        var sourcePlayer by Player optionalReferencedOn Actions.source_player
        var extraData by Actions.extra_data
    }
}