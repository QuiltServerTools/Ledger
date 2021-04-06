package us.potatoboy.ledger.database

import com.mojang.authlib.GameProfile
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.SearchResults
import us.potatoboy.ledger.registry.ActionRegistry
import us.potatoboy.ledger.utility.NbtUtils
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.math.ceil

object DatabaseManager {
    private val databaseFile = File(FabricLoader.getInstance().gameDir.toFile(), "ledger.sqlite")
    private val database = Database.connect(
        url = "jdbc:sqlite:${databaseFile.path.replace('\\', '/')}",
    )

    fun ensureTables() = transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.ObjectIdentifiers,
            Tables.Sources,
            Tables.Worlds
        )
        Ledger.logger.info("Tables created")
    }

    fun insertActions(actions: Collection<ActionType>) {
        transaction {
            addLogger(StdOutSqlLogger)

            for (action in actions) {
                Tables.Action.new {
                    actionIdentifier = getActionId(action.identifier)!!
                    timestamp = action.timestamp
                    x = action.pos.x
                    y = action.pos.y
                    z = action.pos.z
                    objectId = action.objectIdentifier.let { getRegistryKey(it)!! }
                    world = getWorld(action.world ?: Ledger.server.overworld.registryKey.value)!!
                    blockState = action.blockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
                    sourceName = Tables.Source[getAndCreateSource(action.sourceName)]
                    sourcePlayer = action.sourceProfile?.let { getPlayer(it.id) }
                    extraData = action.extraData
                }
            }
        }
    }

    fun searchActions(params: ActionSearchParams, page: Int, source: ServerCommandSource): SearchResults {
        val actionTypes = mutableListOf<ActionType>()
        var totalActions: Long = 0

        transaction {
            addLogger(StdOutSqlLogger)

            var query: Query
            try {
                query = buildQuery(params, source)
            } catch (e: IllegalArgumentException) {
                return@transaction
            }

            totalActions = query.copy().count()
            if (totalActions == 0L) return@transaction

            query = query.orderBy(Tables.Actions.id, SortOrder.DESC)
            query = query.limit(
                Ledger.PAGESIZE,
                (Ledger.PAGESIZE * (page - 1)).toLong()
            ).withDistinct()

            val actions = Tables.Action.wrapRows(query).toList()

            actionTypes.addAll(daoToActionType(actions))
        }

        val totalPages = ceil(totalActions.toDouble() / Ledger.PAGESIZE.toDouble()).toInt()

        return SearchResults(actionTypes, params, page, totalPages)
    }

    fun rollbackActions(params: ActionSearchParams, source: ServerCommandSource): List<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        transaction {
            val query: Query
            try {
                query = buildQuery(params, source)
                    .andWhere { Tables.Actions.rolledBack eq false }
                    .orderBy(Tables.Actions.id, SortOrder.DESC)
            } catch (e: IllegalArgumentException) {
                return@transaction
            }

            val actions = Tables.Action.wrapRows(query).toList()
            for (action in actions) {
                action.rolledBack = true
            }

            actionTypes.addAll(daoToActionType(actions))
        }

        return actionTypes
    }

    fun previewActions(params: ActionSearchParams, source: ServerCommandSource): List<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        transaction {
            val query: Query
            try {
                query = buildQuery(params, source)
                    .andWhere { Tables.Actions.rolledBack eq false }
                    .orderBy(Tables.Actions.id, SortOrder.DESC)
            } catch (e: IllegalArgumentException) {
                return@transaction
            }

            val actions = Tables.Action.wrapRows(query).toList()

            actionTypes.addAll(daoToActionType(actions))
        }

        return actionTypes
    }

    private fun daoToActionType(actions: List<Tables.Action>): List<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        for (action in actions) {
            val typeSupplier = ActionRegistry.getType(action.actionIdentifier.actionIdentifier)
            if (typeSupplier == null) {
                Ledger.logger.warn("Unknown action type ${action.actionIdentifier.actionIdentifier}")
                continue
            }

            val type = typeSupplier.get()
            type.timestamp = action.timestamp
            type.pos = BlockPos(action.x, action.y, action.z)
            type.world = action.world.identifier
            type.objectIdentifier = action.objectId.identifier
            type.blockState = action.blockState?.let {
                NbtUtils.blockStateFromProperties(
                    StringNbtReader.parse(it),
                    action.objectId.identifier
                )
            }
            type.sourceName = action.sourceName.name
            type.sourceProfile = action.sourcePlayer?.let { GameProfile(it.playerId, it.playerName) }
            type.extraData = action.extraData
            type.rolledBack = action.rolledBack

            actionTypes.add(type)
        }

        return actionTypes
    }

    private fun buildQuery(params: ActionSearchParams, source: ServerCommandSource): Query {
        val query = Tables.Actions
            //TODO figure out why this doesn't work .leftJoin(Tables.Players)
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .innerJoin(Tables.ObjectIdentifiers)
            .innerJoin(Tables.Sources)
            .selectAll()

        if (params.min != null && params.max != null) {
            query.andWhere { Tables.Actions.x.between(params.min.x, params.max.x) }
            query.andWhere { Tables.Actions.y.between(params.min.y, params.max.y) }
            query.andWhere { Tables.Actions.z.between(params.min.z, params.max.z) }
        }

        if (params.time != null) {
            query.andWhere { Tables.Actions.timestamp.greaterEq(Instant.now().minus(params.time)) }
        }

        addParameters(
            query,
            params.sourceNames,
            Tables.Sources.name
        )

        addParameters(
            query,
            params.actions,
            Tables.ActionIdentifiers.actionIdentifier
        )

        addParameters(
            query,
            params.worlds?.map { it.toString() },
            Tables.Worlds.identifier
        )

        addParameters(
            query,
            params.objects?.map { it.toString() },
            Tables.ObjectIdentifiers.identifier
        )

        if (!addNullableParameters(
                query,
                params.sourcePlayerNames,
                Tables.Actions.sourcePlayer,
                ::getPlayer,
                source
            )
        ) throw IllegalArgumentException()

        return query
    }

    private fun <E> addParameters(
        query: Query,
        paramSet: Collection<E>?,
        column: Column<E>
    ) {
        if (paramSet != null) {
            paramSet.forEachIndexed { index, param ->
                if (index == 0) {
                    query.andWhere { column eq param }
                } else {
                    query.orWhere { column eq param }
                }
            }
        }
    }

    private fun <E> addNullableParameters(
        query: Query,
        paramSet: Set<E>?,
        column: Column<EntityID<Int>?>,
        getEntity: (e: E) -> IntEntity?,
        source: ServerCommandSource
    ): Boolean {
        paramSet?.forEachIndexed { index, param ->
            val paramEntity = getEntity(param)
            return if (paramEntity == null) {
                source.sendError(TranslatableText("error.ledger.unknown_param", param))
                false
            } else {
                if (index == 0) {
                    query.andWhere { column eq paramEntity.id }
                } else {
                    query.orWhere { column eq paramEntity.id }
                }

                true
            }
        }

        return true
    }

    fun insertActionId(id: String) {
        transaction {
            if (Tables.ActionIdentifier.find { Tables.ActionIdentifiers.actionIdentifier eq id }.empty()) {
                val actionIdentifier = Tables.ActionIdentifier.new {
                    actionIdentifier = id
                }
            }
        }
    }

    //TODO cache in a map maybe?
    private fun getActionId(id: String): Tables.ActionIdentifier? {
        //Tables.ActionIdentifier.find { Tables.ActionIdentifiers.action_identifier eq id }.firstOrNull()!!

        val query = Tables.ActionIdentifiers.select {
            Tables.ActionIdentifiers.actionIdentifier eq id
        }.limit(1)

        return Tables.ActionIdentifier.wrapRows(query).firstOrNull()
    }

    fun insertObject(identifier: Identifier) {
        transaction {
            Tables.ObjectIdentifiers.insertIgnore {
                it[this.identifier] = identifier.toString()
            }
        }
    }

    fun insertWorld(identifier: Identifier) {
        transaction {
            Tables.Worlds.insertIgnore {
                it[this.identifier] = identifier.toString()
            }
        }
    }

    private fun getAndCreateSource(source: String) =
        transaction {
            Tables.Sources.insertIgnore {
                it[name] = source
            }

            getSource(source)!!.id
        }

    private fun getSource(source: String) =
        transaction {
            Tables.Source.find { Tables.Sources.name eq source }.firstOrNull()
        }


    //TODO cache in a map maybe?
    private fun getRegistryKey(identifier: Identifier) = transaction {
        Tables.ObjectIdentifier.find { Tables.ObjectIdentifiers.identifier eq identifier.toString() }.limit(1)
            .firstOrNull()
    }

    private fun getWorld(identifier: Identifier) = transaction {
        Tables.World.find { Tables.Worlds.identifier eq identifier.toString() }.limit(1).firstOrNull()
    }

    fun registerKeys(identifiers: Set<Identifier>) {
        transaction {
            Tables.ObjectIdentifiers.batchInsert(identifiers) { identifier ->
                this[Tables.ObjectIdentifiers.identifier] = identifier.toString()
            }
        }
    }

    fun addPlayer(uuid: UUID, name: String) {
        transaction {
            addLogger(StdOutSqlLogger)

            val player = Tables.Player.find { Tables.Players.playerId eq uuid }.firstOrNull()

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
        return Tables.Player.find { Tables.Players.playerId eq playerId }.firstOrNull()
    }

    fun getPlayer(playerName: String): Tables.Player? {
        return Tables.Player.find { Tables.Players.playerName.lowerCase() eq playerName }.firstOrNull()
    }
}
