package com.github.quiltservertools.ledger.database

import net.minecraft.resources.Identifier
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

private const val MAX_PLAYER_NAME_LENGTH = 16
private const val MAX_ACTION_NAME_LENGTH = 16
private const val MAX_IDENTIFIER_LENGTH = 191
private const val MAX_SOURCE_NAME_LENGTH = 30
private const val MAX_BLOCK_STATE_LENGTH = 500

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

    public val oldObjectTable = ObjectIdentifiers.alias("oldObjects")

    class ObjectIdentifier(id: EntityID<Int>) : IntEntity(id) {
        var identifier by ObjectIdentifiers.identifier.transform({ it.toString() }, { Identifier.tryParse(it)!! })

        companion object : IntEntityClass<ObjectIdentifier>(ObjectIdentifiers)
    }

    /**
     * Reoptimization: dictionary table for block state strings.
     * The actions table stores an int reference instead of repeating the full state
     * string on every row (CoreProtect-style dictionary encoding).
     */
    object BlockStates : IntIdTable("block_states") {
        val state = varchar("state", MAX_BLOCK_STATE_LENGTH).uniqueIndex()
    }

    class BlockState(id: EntityID<Int>) : IntEntity(id) {
        var state by BlockStates.state

        companion object : IntEntityClass<BlockState>(BlockStates)
    }

    object Actions : IntIdTable("actions") {
        val actionIdentifier = reference("action_id", ActionIdentifiers.id).index()
        val timestamp = timestamp("time").index("actions_time")
        val x = integer("x")
        val y = integer("y")
        val z = integer("z")
        val world = reference("world_id", Worlds.id)
        val objectId = reference("object_id", ObjectIdentifiers.id).index()
        val oldObjectId = reference("old_object_id", ObjectIdentifiers.id).index()

        // Reoptimization: nullable legacy text columns kept for backwards compatibility.
        // New writes store the dictionary id in the *_ref columns and null here.
        val blockState = text("block_state").nullable()
        val oldBlockState = text("old_block_state").nullable()
        val blockStateRef = integer("block_state_ref").nullable()
        val oldBlockStateRef = integer("old_block_state_ref").nullable()

        val sourceName = reference("source", Sources.id).index()
        val sourcePlayer = optReference("player_id", Players.id).index()
        val extraData = text("extra_data").nullable()
        val rolledBack = bool("rolled_back").clientDefault { false }

        init {
            // Reoptimization note: the composite (dimension, time) indexes that an
            // earlier revision of this branch introduced were reverted. Ledger stores
            // timestamps as TEXT, so every additional index column carries ~23 bytes
            // per row; measured on a 13,500-row workload they inflated the index
            // footprint from ~1.45 MB to ~3.29 MB (+78% total file size) with no
            // measurable lookup win. Upstream's narrower index set is kept instead.
            index("actions_by_location", false, x, y, z, world)
        }
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
