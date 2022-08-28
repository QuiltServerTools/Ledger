package com.github.quiltservertools.ledger.database

import net.minecraft.util.Identifier
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.text
import org.ktorm.schema.timestamp
import org.ktorm.schema.varchar
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

object Tables {
    object Players : Table<Player>("players") {
        val id = int("id").primaryKey()
        // see https://github.com/kotlin-orm/ktorm/issues/418
        val playerId = varchar("player_id").transform({ UUID.nameUUIDFromBytes(it.toByteArray()) }, { String(it.toBytes()) })
        val playerName = varchar("player_name")
        val firstJoin = timestamp("first_join")
        val lastJoin = timestamp("last_join")
    }

    interface Player : Entity<Player> {
        val id: Int
        var playerId: UUID
        var playerName: String
        var firstJoin: Instant
        var lastJoin: Instant

        companion object : Entity.Factory<Player>()
    }

    object ActionIdentifiers : Table<ActionIdentifier>("ActionIdentifiers") {
        val id = int("id").primaryKey()
        val actionIdentifier = varchar("action_identifier").bindTo { it.identifier }
    }

    interface ActionIdentifier : Entity<ActionIdentifier> {
        val id: Int
        var identifier: String

        companion object : Entity.Factory<ActionIdentifier>()
    }

    open class ObjectIdentifiers(alias: String? = null) : Table<ObjectIdentifier>("ObjectIdentifiers", alias) {
        val id = int("id").primaryKey()
        val identifier = varchar("identifier").transform({ Identifier.tryParse(it)!! }, { it.toString() })

        override fun aliased(alias: String) = ObjectIdentifiers(alias)
        companion object : ObjectIdentifiers() {
            val oldObjectsTable = this.aliased("oldObjects")
        }
    }

    interface ObjectIdentifier : Entity<ObjectIdentifier> {
        val id: Int
        var identifier: Identifier

        companion object : Entity.Factory<ObjectIdentifier>()
    }

    object Actions : Table<Action>("actions") {
        val id = int("id").primaryKey()
        val actionIdentifier = int("action_id").references(ActionIdentifiers) { it.actionIdentifier }
        val timestamp = datetime("time").transform({ it.toInstant(ZoneOffset.UTC) }, { LocalDateTime.ofInstant(it, ZoneOffset.UTC) })
        val x = int("x")
        val y = int("y")
        val z = int("z")
        val world = int("world_id").references(Worlds) { it.world }
        val objectId = int("object_id").references(ObjectIdentifiers) { it.objectId }
        val oldObjectId = int("old_object_id").references(ObjectIdentifiers) { it.oldObjectId }
        val blockState = text("block_state")
        val oldBlockState = text("old_block_state")
        val sourceName = int("source").references(Sources) { it.sourceName }
        val sourcePlayer = int("player_id").references(Players) { it.sourcePlayer }
        val extraData = text("extra_data")
        val rolledBack = boolean("rolled_back")

//        init {
//            index("actions_by_location", false, x, y, z, world)
//        }
    }

    interface Action : Entity<Action> {
        val id: Int
        var actionIdentifier: ActionIdentifier
        var timestamp: Instant
        var x: Int
        var y: Int
        var z: Int
        var world: World
        var objectId: ObjectIdentifier
        var oldObjectId: ObjectIdentifier
        var blockState: String?
        var oldBlockState: String?
        var sourceName: Source
        var sourcePlayer: Player?
        var extraData: String?
        var rolledBack: Boolean

        companion object : Entity.Factory<Action>()
    }

    object Sources : Table<Source>("sources") {
        val id = int("id").primaryKey()
        val name = varchar("name")
    }

    interface Source : Entity<Source> {
        val id: Int
        var name: String

        companion object : Entity.Factory<Source>()
    }

    object Worlds : Table<World>("worlds") {
        val id = int("id").primaryKey()
        val identifier = varchar("identifier").transform({ Identifier.tryParse(it)!! }, { it.toString() })
    }

    interface World : Entity<World> {
        val id: Int
        var identifier: Identifier

        companion object : Entity.Factory<World>()
    }

    val Database.actions get() = this.sequenceOf(Actions)
    val Database.actionIdentifiers get() = this.sequenceOf(ActionIdentifiers)
    val Database.players get() = this.sequenceOf(Players)
    val Database.objects get() = this.sequenceOf(ObjectIdentifiers)
    val Database.sources get() = this.sequenceOf(Sources)
    val Database.worlds get() = this.sequenceOf(Worlds)

    fun UUID.toBytes(): ByteArray {
        @Suppress("MagicNumber")
        val buffer = ByteBuffer.wrap(ByteArray(16))

        buffer.putLong(this.mostSignificantBits)
        buffer.putLong(this.leastSignificantBits)

        return buffer.array()
    }
}
