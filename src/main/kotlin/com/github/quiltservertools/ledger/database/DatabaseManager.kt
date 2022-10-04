package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.api.ExtensionManager
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.logInfo
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.Negatable
import com.github.quiltservertools.ledger.utility.PlayerResult
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
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
import java.time.temporal.ChronoUnit
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
                try {
                    execute {
                        insertAction(it)
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                    logWarn("Exception occurred while attempting to commit action. Skipping.", e)
                }
            }
        }
    }

    fun setValues(file: File, server: MinecraftServer) {
        if (ExtensionManager.getDatabaseExtensionOptional().isPresent) {
            // Extension present, load database from it
            database = ExtensionManager.getDatabaseExtensionOptional().get().getDatabase(server)
        } else {
            // No database extension is present, load normally
            databaseFile = file
            database = Database.connect(
                url = "jdbc:sqlite:${databaseFile.path.replace('\\', '/')}",
            )
        }
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

    fun autoPurge() {
        if (config[DatabaseSpec.autoPurgeDays] > 0) {
            Ledger.launch {
                execute {
                    Ledger.logger.info("Purging actions older than ${config[DatabaseSpec.autoPurgeDays]} days")
                    val deleted = Tables.Actions.deleteWhere {
                        Tables.Actions.timestamp lessEq Instant.now()
                            .minus(config[DatabaseSpec.autoPurgeDays].toLong(), ChronoUnit.DAYS)
                    }
                    Ledger.logger.info("Successfully purged $deleted actions")
                }
            }
        }
    }

    suspend fun searchActions(params: ActionSearchParams, page: Int): SearchResults = execute {
        return@execute selectActionsSearch(params, page)
    }

    suspend fun countActions(params: ActionSearchParams): Long = execute {
        return@execute countActions(params)
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

        if (params.bounds != null) {
            query.andWhere { Tables.Actions.x.between(params.bounds.minX, params.bounds.maxX) }
            query.andWhere { Tables.Actions.y.between(params.bounds.minY, params.bounds.maxY) }
            query.andWhere { Tables.Actions.z.between(params.bounds.minZ, params.bounds.maxZ) }
        }


        if (params.before != null && params.after != null) {
            query.andWhere { Tables.Actions.timestamp.greaterEq(params.after) }
                .andWhere { Tables.Actions.timestamp.lessEq(params.before) }
        } else if (params.before != null) {
            query.andWhere { Tables.Actions.timestamp.lessEq(params.before) }
        } else if (params.after != null) {
            query.andWhere { Tables.Actions.timestamp.greaterEq(params.after) }
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
            params.worlds?.map {
                if (it.allowed) {
                    Negatable.allow(it.property.toString())
                } else {
                    Negatable.deny(it.property.toString())
                }
            },
            Tables.Worlds.identifier
        )

        addParameters(
            query,
            params.objects?.map {
                if (it.allowed) {
                    Negatable.allow(it.property.toString())
                } else {
                    Negatable.deny(it.property.toString())
                }
            },
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
        paramSet: Collection<Negatable<E>>?,
        column: Column<E>,
        orColumn: Column<E>? = null
    ) {
        fun addAllowedParameters(
            allowed: Collection<E>,
        ) {
            if (allowed.isEmpty()) return

            var operator = if (orColumn != null) {
                Op.build { column eq allowed.first() or (orColumn eq allowed.first()) }
            } else {
                Op.build { column eq allowed.first() }
            }

            allowed.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.or { column eq param or (orColumn eq param) }
                } else {
                    operator.or { column eq param }
                }
            }

            query.andWhere { operator }
        }

        fun addDeniedParameters(
            denied: Collection<E>
        ) {
            if (denied.isEmpty()) return

            var operator = if (orColumn != null) {
                Op.build { column neq denied.first() and (orColumn neq denied.first()) }
            } else {
                Op.build { column neq denied.first() or column.isNull() }
            }

            denied.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.and { column neq param and (orColumn neq param) }
                } else {
                    operator.and { column neq param or column.isNull() }
                }
            }

            query.andWhere { operator }
        }

        if (paramSet.isNullOrEmpty()) return

        addAllowedParameters(paramSet.filter { it.allowed }.map { it.property })
        addDeniedParameters(paramSet.filterNot { it.allowed }.map { it.property })
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
            while (Ledger.server.overworld?.savingDisabled != false) {
                delay(timeMillis = 1000)
            }

            newSuspendedTransaction(db = database) {
                body(this)
            }
        }

    suspend fun purgeActions(params: ActionSearchParams) {
        execute {
            purgeActions(params)
        }
    }

    suspend fun searchPlayers(players: Set<GameProfile>): List<PlayerResult> =
        execute {
            return@execute selectPlayers(players)
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

    private fun Transaction.countActions(params: ActionSearchParams): Long = buildQuery(params).copy().count()

    private fun Transaction.selectActionsPreview(
        params: ActionSearchParams,
        type: Preview.Type
    ): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val isRestore = type == Preview.Type.RESTORE

        val query = buildQuery(params)
            .andWhere { Tables.Actions.rolledBack eq isRestore }
            .orderBy(Tables.Actions.id, if (isRestore) SortOrder.ASC else SortOrder.DESC)

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
            .orderBy(Tables.Actions.id, SortOrder.ASC)

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

    private fun Transaction.selectPlayers(players: Set<GameProfile>): List<PlayerResult> {
        val query = Tables.Players.selectAll()

        addParameters(query, players.map { Negatable.allow(it.id) }, Tables.Players.playerId)

        return Tables.Player.wrapRows(query).toList().map { PlayerResult.fromRow(it) }
    }
}
