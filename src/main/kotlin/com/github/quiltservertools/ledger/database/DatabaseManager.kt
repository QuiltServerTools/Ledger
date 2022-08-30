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
import com.github.quiltservertools.ledger.database.Tables.actionIdentifiers
import com.github.quiltservertools.ledger.database.Tables.objects
import com.github.quiltservertools.ledger.database.Tables.players
import com.github.quiltservertools.ledger.database.Tables.sources
import com.github.quiltservertools.ledger.database.Tables.worlds
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.Negatable
import com.github.quiltservertools.ledger.utility.PlayerResult
import com.google.common.collect.Queues
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.BlockPos
import org.ktorm.database.Database
import org.ktorm.database.Transaction
import org.ktorm.dsl.Query
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.between
import org.ktorm.dsl.delete
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.greaterEq
import org.ktorm.dsl.inList
import org.ktorm.dsl.innerJoin
import org.ktorm.dsl.isNull
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.lessEq
import org.ktorm.dsl.like
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.neq
import org.ktorm.dsl.or
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.selectDistinct
import org.ktorm.dsl.update
import org.ktorm.dsl.where
import org.ktorm.entity.add
import org.ktorm.entity.find
import org.ktorm.schema.Column
import org.ktorm.schema.ColumnDeclaring
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.math.ceil

const val BATCH_SIZE = 1000
const val BATCH_WAIT = 30L

object DatabaseManager {
    internal lateinit var database: Database
    var dbMutex: Mutex? = null
    val databaseType: String
        // FIXME
        get() = database.dialect.javaClass.typeName

    private val actions = LinkedBlockingQueue<ActionType>()
//    private val _actions = MutableSharedFlow<ActionType>(extraBufferCapacity = Channel.UNLIMITED)
//    val actions = _actions.asSharedFlow()

    init {
        Ledger.launch {
            while (true) {
                val queuedActions = ArrayList<ActionType>(BATCH_SIZE)
                Queues.drain(
                    actions,
                    queuedActions,
                    BATCH_SIZE,
                    BATCH_WAIT,
                    TimeUnit.SECONDS
                ) // TODO make queue drain size and timeout config

                if (queuedActions.isEmpty()) continue
                execute {
                    insertActions(queuedActions)
                }
            }
        }
    }

    fun setup() {
        if (ExtensionManager.getDataSource() != null) {
            // Extension present, load database from it
            database = Database.connect(ExtensionManager.getDataSource()!!)
        } else {
            // No database extension is present, load normally
            database = Database.connect(
                url = "jdbc:sqlite:${Ledger.server.getSavePath(WorldSavePath.ROOT).resolve("ledger.sqlite").pathString.replace('\\', '/')}",
            )
            dbMutex = Mutex()
        }
    }

    @Suppress("StringLiteralDuplication")
    fun ensureTables() {
        val statements = listOf(
            "CREATE TABLE players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_id BINARY(16) NOT NULL, " +
                    "player_name VARCHAR(16) NOT NULL, " +
                    "first_join TEXT NOT NULL, " +
                    "last_join TEXT NOT NULL" +
            ");",
            "CREATE TABLE worlds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "identifier VARCHAR(191) NOT NULL" +
            ");",
            "CREATE TABLE ObjectIdentifiers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "identifier VARCHAR(191) NOT NULL" +
            ");",
            "CREATE TABLE sources (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "\"name\" VARCHAR(30) NOT NULL" +
            ");",
            "CREATE TABLE actions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "action_id INT NOT NULL, \"time\" TEXT NOT NULL, " +
                    "x INT NOT NULL, " +
                    "y INT NOT NULL, " +
                    "z INT NOT NULL, " +
                    "world_id INT NOT NULL, " +
                    "object_id INT NOT NULL, " +
                    "old_object_id INT NOT NULL, " +
                    "block_state TEXT NULL, " +
                    "old_block_state TEXT NULL, " +
                    "\"source\" INT NOT NULL, " +
                    "player_id INT NULL, " +
                    "extra_data TEXT NULL, " +
                    "rolled_back BOOLEAN NOT NULL, " +
                    "CONSTRAINT fk_actions_action_id__id FOREIGN KEY (action_id) REFERENCES ActionIdentifiers(id) ON DELETE RESTRICT ON UPDATE RESTRICT, " +
                    "CONSTRAINT fk_actions_world_id__id FOREIGN KEY (world_id) REFERENCES worlds(id) ON DELETE RESTRICT ON UPDATE RESTRICT, " +
                    "CONSTRAINT fk_actions_object_id__id FOREIGN KEY (object_id) REFERENCES ObjectIdentifiers(id) ON DELETE RESTRICT ON UPDATE RESTRICT, " +
                    "CONSTRAINT fk_actions_old_object_id__id FOREIGN KEY (old_object_id) REFERENCES ObjectIdentifiers(id) ON DELETE RESTRICT ON UPDATE RESTRICT, " +
                    "CONSTRAINT fk_actions_source__id FOREIGN KEY (\"source\") REFERENCES sources(id) ON DELETE RESTRICT ON UPDATE RESTRICT, " +
                    "CONSTRAINT fk_actions_player_id__id FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT" +
            ");",
            "CREATE UNIQUE INDEX players_player_id ON players (player_id);",
            "CREATE UNIQUE INDEX ActionIdentifiers_action_identifier ON ActionIdentifiers (action_identifier);",
            "CREATE UNIQUE INDEX worlds_identifier ON worlds (identifier);",
            "CREATE UNIQUE INDEX ObjectIdentifiers_identifier ON ObjectIdentifiers (identifier);",
            "CREATE UNIQUE INDEX sources_name ON sources (\"name\");",
            "CREATE INDEX actions_by_location ON actions (x, y, z, world_id);"
        )

        Ledger.launch {
            execute {
                statements.forEach {
                    this.connection.prepareStatement(it).execute()
                }
            }
        }
    }

    fun autoPurge() {
        if (config[DatabaseSpec.autoPurgeDays] > 0) {
            Ledger.launch {
                execute {
                    Ledger.logger.info("Purging actions older than ${config[DatabaseSpec.autoPurgeDays]} days")
                    val deleted = database.delete(Tables.Actions) {
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
        return@execute countActionsTransaction(params)
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

    private fun queryToActionTypes(actions: Query): List<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        for (action in actions) {
            val typeSupplier = ActionRegistry.getType(action[Tables.ActionIdentifiers.actionIdentifier]!!)
            if (typeSupplier == null) {
                logWarn("Unknown action type ${action[Tables.ActionIdentifiers.actionIdentifier]}")
                continue
            }

            val type = typeSupplier.get()
            type.timestamp = action[Tables.Actions.timestamp]!!
            type.pos = BlockPos(action[Tables.Actions.x]!!, action[Tables.Actions.y]!!, action[Tables.Actions.z]!!)
            type.world = action[Tables.Worlds.identifier]
            type.objectIdentifier = action[Tables.ObjectIdentifiers.identifier]!!
            type.oldObjectIdentifier = action[Tables.ObjectIdentifiers.oldObjectsTable.identifier]!!
            type.blockState = action[Tables.Actions.blockState]?.let {
                NbtUtils.blockStateFromProperties(
                    StringNbtReader.parse(it),
                    type.objectIdentifier
                )
            }
            type.oldBlockState = action[Tables.Actions.oldBlockState]?.let {
                NbtUtils.blockStateFromProperties(
                    StringNbtReader.parse(it),
                    type.oldObjectIdentifier
                )
            }
            type.sourceName = action[Tables.Sources.name]!!
            type.sourceProfile = action[Tables.Players.playerId]?.let { GameProfile(it, action[Tables.Players.playerName]) }
            type.extraData = action[Tables.Actions.extraData]
            type.rolledBack = action[Tables.Actions.rolledBack] ?: false

            actionTypes.add(type)
        }

        return actionTypes
    }

    @Suppress("ClassOrdering")
    // to make them declared (SELECT * causes Confused column name exception)
    private val allColumnsForActionQuery: Collection<ColumnDeclaring<*>> = Tables.Actions.columns + listOf(
        Tables.ActionIdentifiers.actionIdentifier,
        Tables.Worlds.identifier,
        Tables.Players.playerId,
        Tables.Players.playerName,
        Tables.ObjectIdentifiers.identifier,
        Tables.ObjectIdentifiers.oldObjectsTable.identifier,
        Tables.Sources.name
    )

    private fun buildQuery(
        params: ActionSearchParams,
        where: ColumnDeclaring<Boolean>? = null,
        isDistinct: Boolean = false,
        columns: Collection<ColumnDeclaring<*>> = allColumnsForActionQuery
    ): Query {
        val oldObjectTable = Tables.ObjectIdentifiers.oldObjectsTable

        val query = database
            .from(Tables.Actions)
            .innerJoin(Tables.ActionIdentifiers, on = Tables.ActionIdentifiers.id eq Tables.Actions.actionIdentifier)
            .innerJoin(Tables.Worlds, on = Tables.Worlds.id eq Tables.Actions.world)
            .leftJoin(Tables.Players, on = Tables.Players.id eq Tables.Actions.sourcePlayer)
            .innerJoin(oldObjectTable, on = oldObjectTable.id eq Tables.Actions.oldObjectId)
            .innerJoin(Tables.ObjectIdentifiers, on = Tables.ObjectIdentifiers.id eq Tables.Actions.objectId)
            .innerJoin(Tables.Sources, on = Tables.Sources.id eq Tables.Actions.sourceName)
            .let {
                if (isDistinct) {
                    it.selectDistinct(columns)
                } else {
                    it.select(columns)
                }
            }

        val wheres = concatParameters(
            where,
            params.bounds?.let {
                Tables.Actions.x.between(IntRange(it.minX, it.maxX + 1)) and
                        Tables.Actions.y.between(IntRange(it.minY, it.maxY + 1)) and
                        Tables.Actions.z.between(IntRange(it.minZ, it.maxZ + 1))
            },
            params.before?.let { Tables.Actions.timestamp.greaterEq(it) },
            params.after?.let { Tables.Actions.timestamp.lessEq(it) },
            generateParameters(params.sourceNames, Tables.Sources.name),
            generateParameters(params.actions, Tables.ActionIdentifiers.actionIdentifier),
            generateParameters(
                params.worlds?.map {
                    if (it.allowed) {
                        Negatable.allow(it.property)
                    } else {
                        Negatable.deny(it.property)
                    }
                },
                Tables.Worlds.identifier
            ),
            generateParameters(
                params.objects?.map {
                    if (it.allowed) {
                        Negatable.allow(it.property)
                    } else {
                        Negatable.deny(it.property)
                    }
                },
                Tables.ObjectIdentifiers.identifier,
                oldObjectTable.identifier
            ),
            generateParameters(params.sourcePlayerNames, Tables.Players.playerName)
        )

        return wheres?.let { query.where(it) } ?: query
    }

    private fun concatParameters(vararg wheres: ColumnDeclaring<Boolean>?): ColumnDeclaring<Boolean>? =
        wheres.filterNotNull().ifEmpty { return null }.reduce { acc, exp -> acc and exp }

    private infix fun ColumnDeclaring<Boolean>?.andMaybeNull(other: ColumnDeclaring<Boolean>?): ColumnDeclaring<Boolean>? = concatParameters(this, other)

    private fun <E: Any> generateParameters(
        paramSet: Collection<Negatable<E>>?,
        column: Column<E>,
        orColumn: Column<E>? = null
    ): ColumnDeclaring<Boolean>? {
        fun addAllowedParameters(
            allowed: Collection<E>,
        ): ColumnDeclaring<Boolean>? {
            if (allowed.isEmpty()) return null

            var operator = if (orColumn != null) {
                column eq allowed.first() or (orColumn eq allowed.first())
            } else {
                column eq allowed.first()
            }

            allowed.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.or(column eq param or (orColumn eq param))
                } else {
                    operator.or(column eq param)
                }
            }

            return operator
        }

        fun addDeniedParameters(
            denied: Collection<E>
        ): ColumnDeclaring<Boolean>? {
            if (denied.isEmpty()) return null

            var operator = if (orColumn != null) {
                column neq denied.first() and (orColumn neq denied.first())
            } else {
                column neq denied.first() or column.isNull()
            }

            denied.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.and(column neq param and (orColumn neq param))
                } else {
                    operator.and(column neq param or column.isNull())
                }
            }

            return operator
        }

        if (paramSet.isNullOrEmpty()) return null

        return addAllowedParameters(paramSet.filter { it.allowed }.map { it.property }) andMaybeNull addDeniedParameters(paramSet.filterNot { it.allowed }.map { it.property })
    }

    fun logAction(action: ActionType) {
        if (action.isBlacklisted()) return

        actions.add(action)
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

    private suspend fun <T : Any?> execute(body: suspend Transaction.() -> T): T {
        suspend fun run(): T {
            while (Ledger.server.overworld?.savingDisabled != false) {
                delay(timeMillis = 1000)
            }

            return database.useTransaction {
                body(it)
            }
        }

        return dbMutex?.withLock { run() } ?: run()
    }

    suspend fun purgeActions(params: ActionSearchParams) {
        execute {
            purgeActionsTransaction(params)
        }
    }

    suspend fun searchPlayers(players: Set<GameProfile>): List<PlayerResult> =
        execute {
            return@execute selectPlayers(players)
        }

    private fun insertActionType(id: String) {
        if (database.actionIdentifiers.find { Tables.ActionIdentifiers.actionIdentifier eq id } == null) {
            val actionIdentifier = Tables.ActionIdentifier {
                identifier = id
            }
            database.actionIdentifiers.add(actionIdentifier)
        }
    }

    private fun insertWorld(id: Identifier) {
        if(database.worlds.find { it.identifier eq id } == null) {
            database.worlds.add(Tables.World { identifier = id })
        }
    }

    private fun insertRegKeys(identifiers: Collection<Identifier>) {
        val filteredIdentifiers = identifiers
                .filter { identifier -> database.objects.find { it.identifier eq identifier } == null }
        if(filteredIdentifiers.isEmpty()) return

        database.batchInsert(Tables.ObjectIdentifiers) {
            filteredIdentifiers.map { id ->
                    item {
                        set(it.identifier, id)
                    }
                }
        }
    }

    private fun insertActions(actions: List<ActionType>) {
        database.batchInsert(Tables.Actions) {
            actions.map { action ->
                item {
                    set(it.actionIdentifier, selectActionId(action.identifier)!!.id)
                    set(it.timestamp, action.timestamp)
                    set(it.x, action.pos.x)
                    set(it.y, action.pos.y)
                    set(it.z, action.pos.z)
                    set(it.objectId, selectRegistryKey(action.objectIdentifier)!!.id)
                    set(it.oldObjectId, selectRegistryKey(action.oldObjectIdentifier)!!.id)
                    set(it.world, selectWorld(action.world ?: Ledger.server.overworld.registryKey.value)!!.id)
                    set(it.blockState, action.blockState?.let { state -> NbtUtils.blockStateToProperties(state)?.asString() })
                    set(it.oldBlockState, action.oldBlockState?.let { state -> NbtUtils.blockStateToProperties(state)?.asString() })
                    set(it.sourceName, insertAndSelectSource(action.sourceName).id)
                    set(it.sourcePlayer, action.sourceProfile?.let { profile -> selectPlayer(profile.id)?.id })
                    set(it.extraData, action.extraData)
                    set(it.rolledBack, false)
                }
            }
        }
    }

    private fun insertPlayer(uuid: UUID, name: String) {
        val player = database.players.find { Tables.Players.playerId eq uuid }

        if (player != null) {
            player.lastJoin = Instant.now()
            player.playerName = name
            player.flushChanges()
        } else {
            val newPlayer = Tables.Player {
                playerId = uuid
                playerName = name
                firstJoin = Instant.now()
                lastJoin = Instant.now()
            }
            database.players.add(newPlayer)
        }
    }

    private fun selectActionsSearch(params: ActionSearchParams, page: Int): SearchResults {
        val query = buildQuery(params, isDistinct = true)
        val totalCount = query.totalRecords

        val actionTypes = queryToActionTypes(
            query.orderBy(Tables.Actions.id.desc())
                .limit(
                    config[SearchSpec.pageSize] * (page - 1),
                    config[SearchSpec.pageSize]
                )
        )

        val totalPages = ceil(totalCount.toDouble() / config[SearchSpec.pageSize].toDouble()).toInt()

        return SearchResults(actionTypes, params, page, totalPages)
    }

    private fun countActionsTransaction(params: ActionSearchParams): Long = buildQuery(params).totalRecords.toLong()

    private fun selectActionsPreview(
        params: ActionSearchParams,
        type: Preview.Type
    ): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val isRestore = type == Preview.Type.RESTORE

        val query = buildQuery(params, Tables.Actions.rolledBack eq isRestore)
            .orderBy(Tables.Actions.id.let { if(isRestore) it.asc() else it.desc() })

        actionTypes.addAll(queryToActionTypes(query))

        return actionTypes
    }

    private fun selectRollbackActions(params: ActionSearchParams): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val query = buildQuery(params, Tables.Actions.rolledBack eq false)

        actionTypes.addAll(queryToActionTypes(query.orderBy(Tables.Actions.id.desc())))

        val ids = query.map { it[Tables.Actions.id]!! }
        database.update(Tables.Actions) {
            set(it.rolledBack, true)
            where {
                it.id inList ids
            }
        }

        return actionTypes
    }

    private fun selectRestoreActions(params: ActionSearchParams): MutableList<ActionType> {
        val actionTypes = mutableListOf<ActionType>()

        val query = buildQuery(params, Tables.Actions.rolledBack eq true)

        actionTypes.addAll(queryToActionTypes(query.orderBy(Tables.Actions.id.asc())))

        val ids = query.map { it[Tables.Actions.id]!! }
        database.update(Tables.Actions) {
            set(it.rolledBack, false)
            where {
                it.id inList ids
            }
        }

        return actionTypes
    }

    private fun selectPlayer(playerId: UUID) =
        database.players.find { Tables.Players.playerId eq playerId }

    private fun selectPlayer(playerName: String) =
        database.players.find { Tables.Players.playerName like playerName }

    private fun insertAndSelectSource(source: String): Tables.Source {
        val sourceDAO = database.sources.find { Tables.Sources.name eq source }

        return if (sourceDAO == null) {
            val newDao = Tables.Source { name = source }
            database.sources.add(newDao)
            newDao
        } else {
            sourceDAO
        }
    }

    private fun selectActionId(id: String) =
        database.actionIdentifiers.find { Tables.ActionIdentifiers.actionIdentifier eq id }

    private fun selectRegistryKey(identifier: Identifier) =
        database.objects.find { Tables.ObjectIdentifiers.identifier eq identifier }

    private fun selectWorld(identifier: Identifier) =
        database.worlds.find { Tables.Worlds.identifier eq identifier }

    private fun purgeActionsTransaction(params: ActionSearchParams) {
        // TODO: better way to do this
        for (action in buildQuery(params, isDistinct = true, columns = listOf(Tables.Actions.id))) {
            database.delete(Tables.Actions) { Tables.Actions.id eq action[Tables.Actions.id]!! }
        }
    }

    private fun selectPlayers(players: Set<GameProfile>): List<PlayerResult> =
        database.from(Tables.Players)
            .select()
            .where(generateParameters(players.map { Negatable.allow(it.id) }, Tables.Players.playerId)!!)
            .map { PlayerResult.fromRow(it) }
}
