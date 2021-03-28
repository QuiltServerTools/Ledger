package us.potatoboy.ledger.database

import com.mojang.authlib.GameProfile
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.registry.LedgerRegistry
import us.potatoboy.ledger.utility.NbtUtils
import java.io.File
import java.time.Instant
import java.util.*

object DatabaseManager {
    private val databaseFile = File(FabricLoader.getInstance().gameDir.toFile(), "ledger.sqlite")
    private val database = Database.connect("jdbc:sqlite:${databaseFile.path.replace('\\', '/')}")

    fun ensureTables() = transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.RegistryKeys
        )
        Ledger.logger.info("Tables created")
    }

    fun insertActions(actions: Collection<ActionType>) {
        transaction {
            addLogger(StdOutSqlLogger)

            for (action in actions) {
                Tables.Action.new {
                    actionIdentifier = getActionId(action.identifier)
                    timestamp = action.timestamp
                    x = action.pos.x
                    y = action.pos.y
                    z = action.pos.z
                    objectId = action.objectIdentifier?.let { getRegistryKey(it) }
                    world = getRegistryKey(action.world ?: Ledger.server.overworld.registryKey.value)
                    blockState = action.blockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
                    sourceName = action.sourceName
                    sourcePlayer = action.sourceProfile?.let { getPlayer(it.id) }
                    extraData = action.extraData
                }
            }
        }
    }

    fun searchActions(params: ActionLookupParams): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        transaction {
            val query = Tables.Actions.selectAll()

            if (params.min != null && params.max != null) {
                query.andWhere { Tables.Actions.x.between(params.min.x, params.max.x) }
                query.andWhere { Tables.Actions.y.between(params.min.y, params.max.y) }
                query.andWhere { Tables.Actions.z.between(params.min.z, params.max.z) }
            }

            if (params.actions != null) {
                params.actions.forEach { actionType ->
                    query.andWhere { Tables.Actions.actionIdentifier eq getActionId(actionType.identifier).id}
                }
            }

            query.orderBy(Tables.Actions.id, SortOrder.DESC)

            val actions = Tables.Action.wrapRows(query).toList()

            for (action in actions) {
                val typeSupplier = LedgerRegistry.getType(action.actionIdentifier.actionIdentifier)
                if (typeSupplier == null) {
                    Ledger.logger.warn("Unknown action type ${action.actionIdentifier.actionIdentifier}")
                    continue
                }

                val type = typeSupplier.get()
                type.timestamp = action.timestamp
                type.pos = BlockPos(action.x, action.y, action.z)
                type.world = action.world.identifier
                type.objectIdentifier = action.objectId.identifier
                //TODO blockstate stuff type.blockState = action.blockState
                type.sourceName = action.sourceName
                type.sourceProfile = action.sourcePlayer?. let { GameProfile(it.playerId, it.playerName) }
                type.extraData = action.extraData

                actionTypes.add(type)
            }
        }

        return actionTypes
    }

    fun insertActionId(id: String) {
        transaction {
            if (Tables.ActionIdentifier.find { Tables.ActionIdentifiers.action_identifier eq id }.empty()) {
                val actionIdentifier = Tables.ActionIdentifier.new {
                    actionIdentifier = id
                }
            }
        }
    }


    //TODO cache in a map maybe?
    fun getActionId(id: String): Tables.ActionIdentifier {
        //Tables.ActionIdentifier.find { Tables.ActionIdentifiers.action_identifier eq id }.firstOrNull()!!

        val query = Tables.ActionIdentifiers.select {
            Tables.ActionIdentifiers.action_identifier eq id
        }.limit(1)

        return Tables.ActionIdentifier.wrapRows(query).first()
    }

    fun registerKey(identifier: Identifier) {
        transaction {
            Tables.RegistryKeys.insertIgnore {
                it[registry_key] = identifier.toString()
            }
        }
    }

    //TODO cache in a map maybe?
    fun getRegistryKey(identifier: Identifier) = transaction {
        Tables.RegistryKey.find { Tables.RegistryKeys.registry_key eq identifier.toString() }.limit(1).first()
    }

    fun registerKeys(identifiers: Set<Identifier>) {
        transaction {
            Tables.RegistryKeys.batchInsert(identifiers) { identifier ->
                this[Tables.RegistryKeys.registry_key] = identifier.toString()
            }
        }
    }

    fun addPlayer(uuid: UUID, name: String) {
        transaction {
            addLogger(StdOutSqlLogger)

            val player = Tables.Player.find { Tables.Players.player_id eq uuid }.firstOrNull()

            if (player != null) {
                player.lastJoin = Instant.now()
                player.playerName = name
            } else {
                Tables.Player.new {
                    this.playerId = uuid
                    this.playerName = name
                }
            }
        }
    }

    //TODO cache in a map maybe?

    fun getPlayer(playerId: UUID): Tables.Player? {
        return Tables.Player.find { Tables.Players.player_id eq playerId }.firstOrNull()
    }
}