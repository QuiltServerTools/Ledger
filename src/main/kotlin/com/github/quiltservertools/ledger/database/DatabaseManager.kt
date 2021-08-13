package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.logInfo
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil

object DatabaseManager {

    // These values are initialised late to allow the database to be created at server start,
    // which means the database file is located in the world folder and allows for per-world databases.
    private lateinit var databaseFile: File
    private lateinit var database: Database
    val dbMutex = Mutex()

    private val _actions = MutableSharedFlow<ActionType>(extraBufferCapacity = Channel.UNLIMITED)
    val actions = _actions.asSharedFlow()

    init {
        Ledger.launch {
            actions.collect {
                execute {
                    insertAction(it)
                }
            }
        }
    }

    fun setValues(file: File) {
        databaseFile = file
        database = Database.connect(
            url = "jdbc:sqlite:${databaseFile.path.replace('\\', '/')}",
        )
    }

    fun ensureTables() = transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.ObjectIdentifiers,
            Tables.Sources,
            Tables.Worlds
        )
        logInfo("Tables created")
    }

    suspend fun searchActions(params: ActionSearchParams, page: Int): SearchResults = execute {
        return@execute selectActionsSearch(params, page)
    }

    suspend fun rollbackActions(params: ActionSearchParams): List<ActionType> = execute {
        return@execute selectRollbackActions(params)
    }

    suspend fun restoreActions(params: ActionSearchParams): List<ActionType> = execute {
        return@execute selectRestoreActions(params)
    }

    suspend fun previewActions(
        params: ActionSearchParams,
        type: Preview.Type
    ): List<ActionType> = execute {
        return@execute selectActionsPreview(params, type)
    }

    private fun daoToActionType(actions: List<Tables.Action>): List<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        for (action in actions) {
            val typeSupplier = ActionRegistry.getType(action.actionIdentifier.identifier)
            if (typeSupplier == null) {
                logWarn("Unknown action type ${action.actionIdentifier.identifier}")
                continue
            }

            val type = typeSupplier.get()
            type.timestamp = action.timestamp
            type.pos = BlockPos(action.x, action.y, action.z)
            type.world = action.world.identifier
            type.objectIdentifier = action.objectId.identifier
            type.oldObjectIdentifier = action.oldObjectId.identifier
            type.blockState = action.blockState?.let {
                NbtUtils.blockStateFromProperties(
                    StringNbtReader.parse(it),
                    action.objectId.identifier
                )
            }
            type.oldBlockState = action.oldBlockState?.let {
                NbtUtils.blockStateFromProperties(
                    StringNbtReader.parse(it),
                    action.oldObjectId.identifier
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

    private fun buildQuery(params: ActionSearchParams): Query {
        val oldObjectTable = Tables.ObjectIdentifiers.alias("oldObjects")

        val query = Tables.Actions
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .leftJoin(Tables.Players)
            .innerJoin(oldObjectTable, { Tables.Actions.oldObjectId }, { oldObjectTable[Tables.ObjectIdentifiers.id] })
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
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
            Tables.ObjectIdentifiers.identifier,
            oldObjectTable[Tables.ObjectIdentifiers.identifier]
        )

        addParameters(
            query,
            params.sourcePlayerNames,
            Tables.Players.playerName
        )

        return query
    }

    private fun <E> addParameters(
        query: Query,
        paramSet: Collection<E>?,
        column: Column<E>
    ) {
        if (paramSet.isNullOrEmpty()) return

        var operator = Op.build { column eq paramSet.first() }

        paramSet.stream().skip(1).forEach { param ->
            operator = operator.or { column eq param }
        }

        query.andWhere { operator }
    }

    private fun <E> addParameters(
        query: Query,
        paramSet: Collection<E>?,
        column: Column<E>,
        orColumn: Column<E>
    ) {
        if (paramSet.isNullOrEmpty()) return

        var operator = Op.build { column eq paramSet.first() or (orColumn eq paramSet.first()) }

        paramSet.stream().skip(1).forEach { param ->
            operator = operator.or { column eq param or (orColumn eq param) }
        }

        query.andWhere { operator }
    }

    fun logAction(action: ActionType) {
        if (action.isBlacklisted()) return

        _actions.tryEmit(action)
    }

    suspend fun registerWorld(identifier: Identifier) =
        execute {
            insertWorld(identifier)
        }

    suspend fun registerActionType(id: String) =
        execute {
            insertActionType(id)
        }

    suspend fun logPlayer(uuid: UUID, name: String) =
        execute {
            insertPlayer(uuid, name)
        }

    suspend fun insertIdentifiers(identifiers: Collection<Identifier>) =
        execute {
            insertRegKeys(identifiers)
        }

    private suspend fun <T : Any?> execute(body: suspend Transaction.() -> T): T =
        dbMutex.withLock {
            newSuspendedTransaction {
                body(this)
            }
        }

    suspend fun purgeActions(params: ActionSearchParams) {
        execute {
            purgeActions(params)
        }
    }

    private fun Transaction.insertActionType(id: String) {
        if (Tables.ActionIdentifier.find { Tables.ActionIdentifiers.actionIdentifier eq id }.empty()) {
            val actionIdentifier = Tables.ActionIdentifier.new {
                identifier = id
            }
        }
    }

    private fun Transaction.insertWorld(identifier: Identifier) {
        Tables.Worlds.insertIgnore {
            it[this.identifier] = identifier.toString()
        }
    }

    private fun Transaction.insertRegKeys(identifiers: Collection<Identifier>) {
        Tables.ObjectIdentifiers.batchInsert(identifiers, true) { identifier ->
            this[Tables.ObjectIdentifiers.identifier] = identifier.toString()
        }
    }

    private fun Transaction.insertAction(action: ActionType) {
        Tables.Action.new {
            actionIdentifier = selectActionId(action.identifier)
            timestamp = action.timestamp
            x = action.pos.x
            y = action.pos.y
            z = action.pos.z
            objectId = selectRegistryKey(action.objectIdentifier)
            oldObjectId = selectRegistryKey(action.oldObjectIdentifier)
            world = selectWorld(action.world ?: Ledger.server.overworld.registryKey.value)
            blockState = action.blockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
            oldBlockState = action.oldBlockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
            sourceName = insertAndSelectSource(action.sourceName)
            sourcePlayer = action.sourceProfile?.let { selectPlayer(it.id) }
            extraData = action.extraData
        }
    }

    private fun Transaction.insertPlayer(uuid: UUID, name: String) {
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

    private fun Transaction.selectActionsSearch(params: ActionSearchParams, page: Int): SearchResults {
        val actionTypes = mutableListOf<ActionType>()
        var totalActions: Long = 0

        var query = buildQuery(params)

        totalActions = query.copy().count()
        if (totalActions == 0L) return SearchResults(actionTypes, params, page, 0)

        query = query.orderBy(Tables.Actions.id, SortOrder.DESC)
        query = query.limit(
            config[SearchSpec.pageSize],
            (config[SearchSpec.pageSize] * (page - 1)).toLong()
        ).withDistinct()

        val actions = Tables.Action.wrapRows(query).toList()

        actionTypes.addAll(daoToActionType(actions))

        val totalPages = ceil(totalActions.toDouble() / config[SearchSpec.pageSize].toDouble()).toInt()

        return SearchResults(actionTypes, params, page, totalPages)
    }

    private fun Transaction.selectActionsPreview(
        params: ActionSearchParams,
        type: Preview.Type
    ): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val query = buildQuery(params)
            .andWhere { Tables.Actions.rolledBack eq (type == Preview.Type.RESTORE) }
            .orderBy(Tables.Actions.id, SortOrder.DESC)

        val actions = Tables.Action.wrapRows(query).toList()
        actionTypes.addAll(daoToActionType(actions))

        return actionTypes
    }

    private fun Transaction.selectRollbackActions(params: ActionSearchParams): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val query = buildQuery(params)
            .andWhere { Tables.Actions.rolledBack eq false }
            .orderBy(Tables.Actions.id, SortOrder.DESC)

        val actions = Tables.Action.wrapRows(query).toList()
        for (action in actions) {
            action.rolledBack = true
        }

        actionTypes.addAll(daoToActionType(actions))

        return actionTypes
    }

    private fun Transaction.selectRestoreActions(params: ActionSearchParams): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val query = buildQuery(params)
            .andWhere { Tables.Actions.rolledBack eq true }
            .orderBy(Tables.Actions.id, SortOrder.DESC)

        val actions = Tables.Action.wrapRows(query).toList()
        for (action in actions) {
            action.rolledBack = false
        }

        actionTypes.addAll(daoToActionType(actions))

        return actionTypes
    }

    private fun Transaction.selectPlayer(playerId: UUID) =
        Tables.Player.find { Tables.Players.playerId eq playerId }.firstOrNull()

    private fun Transaction.selectPlayer(playerName: String) =
        Tables.Player.find { Tables.Players.playerName.lowerCase() eq playerName }.firstOrNull()

    private fun Transaction.insertAndSelectSource(source: String): Tables.Source {
        var sourceDAO = Tables.Source.find { Tables.Sources.name eq source }.firstOrNull()

        if (sourceDAO == null) {
            sourceDAO = Tables.Source[Tables.Sources.insertAndGetId {
                it[name] = source
            }]
        }

        return sourceDAO
    }

    private fun Transaction.selectActionId(id: String) =
        Tables.ActionIdentifier.find { Tables.ActionIdentifiers.actionIdentifier eq id }.first()

    private fun Transaction.selectRegistryKey(identifier: Identifier) =
        Tables.ObjectIdentifier.find { Tables.ObjectIdentifiers.identifier eq identifier.toString() }.limit(1).first()

    private fun Transaction.selectWorld(identifier: Identifier) =
        Tables.World.find { Tables.Worlds.identifier eq identifier.toString() }.limit(1).first()

    private fun Transaction.purgeActions(params: ActionSearchParams) {
        val query = buildQuery(params)
        val actions = Tables.Action.wrapRows(query).toList()
        actions.forEach { action ->
            action.delete()
        }
    }
}
