package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.config.getDatabasePath
import com.github.quiltservertools.ledger.logInfo
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.Negatable
import com.github.quiltservertools.ledger.utility.PlayerResult
import com.google.common.collect.BiMap
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import net.minecraft.server.players.NameAndId
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.EqOp
import org.jetbrains.exposed.v1.core.GreaterOp
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.LessOp
import org.jetbrains.exposed.v1.core.LiteralOp
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.orWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteDataSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Function
import javax.sql.DataSource
import kotlin.io.path.pathString
import kotlin.math.ceil

const val MAX_QUERY_RETRIES = 10
const val MIN_RETRY_DELAY = 1000L
const val MAX_RETRY_DELAY = 300_000L
private const val MAX_EXTRA_DATA_BYTES = 65_535

// Reoptimization: TTL for the search count cache
private const val COUNT_CACHE_TTL_MS = 30_000L
private const val COUNT_CACHE_MAX_ENTRIES = 256

// Reoptimization: SQLite tuning (WAL + NORMAL + generous busy timeout)
private const val SQLITE_BUSY_TIMEOUT_MS = 10_000

// Reoptimization: keyset-pagination helpers. SqlExpressionBuilder is deprecated (ERROR-level)
// in Exposed 1.0.0, so comparison predicates for cursor pagination are built directly.
private fun intLiteral(value: Int) = LiteralOp(IntegerColumnType(), value)

// Reoptimization: kept as a single facade object so the fork stays drop-in with
// upstream call sites; splitting it would ripple through every caller.
@Suppress("LargeClass")
object DatabaseManager {

    // These values are initialised late to allow the database to be created at server start,
    // which means the database file is located in the world folder and allows for per-world databases.
    private lateinit var database: Database
    val databaseType: String
        get() = database.dialect.name

    private val cache = DatabaseCacheService

    // Reoptimization: kept so vacuumDatabase() can open an autocommit connection
    // (VACUUM cannot run inside a transaction).
    private var compactDataSource: DataSource? = null
    private var databaseContext = Dispatchers.IO + CoroutineName("Ledger Database")
    private val ledgerLogger = object : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            Ledger.logger.info("SQL: ${context.expandArgs(transaction)}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun setup(dataSource: DataSource?) {
        if (dataSource == null) {
            val default = getDefaultDatasource()
            compactDataSource = default
            database = Database.connect(default)
            databaseContext = newSingleThreadContext("Ledger Database")
        } else {
            compactDataSource = dataSource
            database = Database.connect(dataSource)
        }
    }

    private fun getDefaultDatasource(): DataSource {
        val dbFilepath = config.getDatabasePath().resolve("ledger.sqlite").pathString
        return SQLiteDataSource(
            SQLiteConfig().apply {
                setJournalMode(SQLiteConfig.JournalMode.WAL)
                // Reoptimization: WAL + NORMAL is the standard high-throughput combo.
                // Commits no longer fsync per transaction (only at checkpoints),
                // which massively reduces batch write latency without risking corruption.
                setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)
                setBusyTimeout(SQLITE_BUSY_TIMEOUT_MS)
            },
        ).apply {
            url = "jdbc:sqlite:$dbFilepath"
        }
    }

    fun ensureTables() = transaction {
        addLogger(ledgerLogger)
        SchemaUtils.create(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.ObjectIdentifiers,
            Tables.Sources,
            Tables.Worlds,
            Tables.BlockStates,
        )

        // Reoptimization: make sure the dictionary-encoding reference columns exist.
        // ALTER TABLE ADD COLUMN (nullable, no default rewrite) is instant on both
        // SQLite and MySQL 8+ INSTANT, and fully backwards compatible.
        ensureBlockStateColumns()

        if (config[DatabaseSpec.updateSchema]) {
            try {
                // Legacy index from upstream; harmless if already present, required pre-1.3.23 databases
                exec("CREATE INDEX IF NOT EXISTS actions_time ON actions(time)")
                // Reoptimization: replace single-column indexes with query-oriented composite ones
                ensurePerformanceIndexes()
            } catch (e: java.sql.SQLException) {
                logWarn("Could not run performance index migration: ${e.message}")
            }
        }
        logInfo("Tables created")
    }

    /**
     * Reoptimization: adds block_state_ref / old_block_state_ref to the actions table
     * when missing (metadata-driven, idempotent).
     */
    private fun JdbcTransaction.ensureBlockStateColumns() {
        val columns = existingColumnNames("actions")
        if ("block_state_ref" !in columns) {
            exec("ALTER TABLE actions ADD COLUMN block_state_ref INTEGER")
        }
        if ("old_block_state_ref" !in columns) {
            exec("ALTER TABLE actions ADD COLUMN old_block_state_ref INTEGER")
        }
    }

    private fun JdbcTransaction.existingColumnNames(table: String): Set<String> {
        val isSQLite = databaseType.equals("SQLite", ignoreCase = true)
        val sql = if (isSQLite) "PRAGMA table_info($table)" else "SHOW COLUMNS FROM $table"
        val column = if (isSQLite) "name" else "Field"
        val names = mutableSetOf<String>()
        exec(sql) { rs ->
            while (rs.next()) names.add(rs.getString(column))
        }
        return names
    }

    /**
     * Reoptimization: creates the composite query-oriented indexes and drops the
     * single-column indexes they supersede (metadata-driven, idempotent, safe on
     * re-run). Only executed when the user opts in via updateSchema.
     */
    private fun JdbcTransaction.ensurePerformanceIndexes() {
        val isSQLite = databaseType.equals("SQLite", ignoreCase = true)

        fun existingIndexes(): Set<String> {
            val names = mutableSetOf<String>()
            if (isSQLite) {
                exec("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='actions'") { rs ->
                    while (rs.next()) names.add(rs.getString("name"))
                }
            } else {
                exec("SHOW INDEX FROM actions") { rs ->
                    while (rs.next()) names.add(rs.getString("Key_name"))
                }
            }
            return names
        }

        val existing = existingIndexes()

        val compositeIndexes = listOf(
            "actions_player_time" to "CREATE INDEX actions_player_time ON actions(player_id, time)",
            "actions_action_time" to "CREATE INDEX actions_action_time ON actions(action_id, time)",
            "actions_object_time" to "CREATE INDEX actions_object_time ON actions(object_id, time)",
            "actions_old_object_time" to "CREATE INDEX actions_old_object_time ON actions(old_object_id, time)",
            "actions_by_location" to "CREATE INDEX actions_by_location ON actions(world_id, x, z, time)",
        )
        for ((name, ddl) in compositeIndexes) {
            if (name !in existing) {
                try {
                    exec(ddl)
                } catch (e: java.sql.SQLException) {
                    logWarn("Could not create index $name: ${e.message}")
                }
            }
        }

        // Drop superseded single-column indexes (only if they still exist).
        // Keep: actions_time (pure time queries), actions_source (source filters).
        val superseded = listOf(
            "actions_object_id",
            "actions_old_object_id",
            "actions_player_id",
            "actions_action_id",
            "actions_x",
            "actions_y",
            "actions_z",
        )
        for (name in superseded) {
            if (name in existing) {
                try {
                    if (isSQLite) {
                        exec("DROP INDEX IF EXISTS $name")
                    } else {
                        exec("DROP INDEX $name ON actions")
                    }
                } catch (e: java.sql.SQLException) {
                    logWarn("Could not drop index $name (harmless, leaving in place): ${e.message}")
                }
            }
        }
    }

    suspend fun setupCache() {
        execute {
            Tables.ActionIdentifier.all().forEach {
                cache.actionIdentifierKeys[it.identifier] = it.id.value
            }
            Tables.World.all().forEach {
                cache.worldIdentifierKeys[it.identifier] = it.id.value
            }
            Tables.ObjectIdentifier.all().forEach {
                cache.objectIdentifierKeys[it.identifier] = it.id.value
            }
            Tables.Source.all().forEach {
                cache.sourceKeys[it.name] = it.id.value
            }
            Tables.BlockState.all().forEach {
                cache.blockStateKeys[it.state] = it.id.value
            }
            Tables.Player.all().forEach {
                cache.playerKeys[it.playerId] = it.id.value
                cache.playernameKeys.forcePut(it.playerName, it.id.value)
            }
        }
    }

    suspend fun autoPurge() {
        if (config[DatabaseSpec.autoPurgeDays] > 0) {
            execute {
                Ledger.logger.info("Purging actions older than ${config[DatabaseSpec.autoPurgeDays]} days")
                val deleted = Tables.Actions.deleteWhere {
                    timestamp lessEq Instant.now().minus(config[DatabaseSpec.autoPurgeDays].toLong(), ChronoUnit.DAYS)
                }
                Ledger.logger.info("Successfully purged $deleted actions")
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
        val actions = selectRollback(params)
        val actionIds = actions.map { it.id }.toSet()
        rollbackActions(actionIds)
        return@execute actions
    }

    suspend fun rollbackActions(actionIds: Set<Int>) = execute {
        return@execute rollbackActions(actionIds)
    }

    suspend fun restoreActions(params: ActionSearchParams): List<ActionType> = execute {
        val actions = selectRestore(params)
        val actionIds = actions.map { it.id }.toSet()
        restoreActions(actionIds)
        return@execute actions
    }

    suspend fun restoreActions(actionIds: Set<Int>) = execute {
        return@execute restoreActions(actionIds)
    }

    suspend fun selectRollback(params: ActionSearchParams): List<ActionType> = execute {
        val query = Tables.Actions
            .selectAll()
            .where(buildQueryParams(params) and (Tables.Actions.rolledBack eq false))
            .orderBy(Tables.Actions.id, SortOrder.DESC)
        return@execute getActionsFromQuery(query)
    }

    suspend fun selectRestore(params: ActionSearchParams): List<ActionType> = execute {
        val query = Tables.Actions
            .selectAll()
            .where(buildQueryParams(params) and (Tables.Actions.rolledBack eq true))
            .orderBy(Tables.Actions.id, SortOrder.ASC)
        return@execute getActionsFromQuery(query)
    }

    // ------------------------------------------------------------------
    // Reoptimization: streaming rollback/restore support
    // ------------------------------------------------------------------

    /**
     * Reoptimization: current maximum actions.id. Used as an upper bound for
     * streaming rollbacks so that blocks logged *by* the rollback itself can
     * never be picked up by subsequent batches of the same rollback.
     */
    suspend fun currentMaxActionId(): Int = execute {
        var maxId = 0
        exec("SELECT COALESCE(MAX(id), 0) FROM ${Tables.Actions.tableName}") { rs ->
            if (rs.next()) maxId = rs.getInt(1)
        }
        maxId
    }

    /**
     * Reoptimization: count of actions matching the params (optionally filtered by
     * rolled-back state). Used for progress reporting without loading rows.
     */
    suspend fun countActionsFor(params: ActionSearchParams, rolledBack: Boolean? = null): Long = execute {
        var op = buildQueryParams(params)
        if (rolledBack != null) {
            op = op and (Tables.Actions.rolledBack eq rolledBack)
        }
        return@execute Tables.Actions.selectAll().where(op).count()
    }

    /**
     * Reoptimization: one batch of rollback candidates ordered newest-first,
     * using a keyset cursor (id < [exclusiveUpperId]) instead of loading the
     * entire result set into memory.
     */
    suspend fun selectRollbackBatch(params: ActionSearchParams, exclusiveUpperId: Int, limit: Int): List<ActionType> =
        execute {
            val query = Tables.Actions
                .selectAll()
                .where(
                    buildQueryParams(params) and (Tables.Actions.rolledBack eq false) and
                        LessOp(Tables.Actions.id, intLiteral(exclusiveUpperId)),
                )
                .orderBy(Tables.Actions.id, SortOrder.DESC)
                .limit(limit)
            return@execute getActionsFromQuery(query)
        }

    /**
     * Reoptimization: one batch of restore candidates ordered oldest-first (keyset cursor).
     */
    suspend fun selectRestoreBatch(params: ActionSearchParams, exclusiveLowerId: Int, limit: Int): List<ActionType> =
        execute {
            val query = Tables.Actions
                .selectAll()
                .where(
                    buildQueryParams(params) and (Tables.Actions.rolledBack eq true) and
                        GreaterOp(Tables.Actions.id, intLiteral(exclusiveLowerId)),
                )
                .orderBy(Tables.Actions.id, SortOrder.ASC)
                .limit(limit)
            return@execute getActionsFromQuery(query)
        }

    /**
     * Reoptimization: rewrites legacy text block states into dictionary references
     * in id-ordered batches, so existing databases shrink once the rows are
     * rewritten and vacuumed. Idempotent: rows already migrated are skipped.
     *
     * @return number of rows migrated
     */
    suspend fun compactBlockStates(
        batchSize: Int = 5000,
        onProgress: suspend (done: Long, total: Long) -> Unit = { _, _ -> },
    ): Long {
        // Reoptimization: native SQL keeps this maintenance path independent of
        // expression-builder API churn. Cursor values are server-generated ints.
        val total = execute {
            var count = 0L
            exec(
                "SELECT COUNT(*) FROM ${Tables.Actions.tableName} " +
                    "WHERE (block_state IS NOT NULL OR old_block_state IS NOT NULL)",
            ) { rs ->
                if (rs.next()) count = rs.getLong(1)
            }
            count
        }
        if (total == 0L) return 0L

        var migrated = 0L
        var cursor = 0
        while (true) {
            // Read batch (materialised before the write transaction starts)
            val rows = execute {
                val batch = mutableListOf<Triple<Int, String?, String?>>()
                exec(
                    "SELECT id, block_state, old_block_state FROM ${Tables.Actions.tableName} " +
                        "WHERE (block_state IS NOT NULL OR old_block_state IS NOT NULL) AND id > $cursor " +
                        "ORDER BY id ASC LIMIT $batchSize",
                ) { rs ->
                    while (rs.next()) {
                        batch.add(
                            Triple(rs.getInt("id"), rs.getString("block_state"), rs.getString("old_block_state")),
                        )
                    }
                }
                batch
            }
            if (rows.isEmpty()) break

            execute {
                for ((id, state, oldState) in rows) {
                    val newRef = state?.let { getOrCreateBlockStateId(it) }
                    val newOldRef = oldState?.let { getOrCreateBlockStateId(it) }
                    Tables.Actions.update({ EqOp(Tables.Actions.id, intLiteral(id)) }) {
                        it[Tables.Actions.blockStateRef] = newRef
                        it[Tables.Actions.oldBlockStateRef] = newOldRef
                        it[Tables.Actions.blockState] = null
                        it[Tables.Actions.oldBlockState] = null
                    }
                }
            }

            migrated += rows.size
            cursor = rows.last().first
            onProgress(migrated, total)
        }
        return migrated
    }

    /**
     * Reoptimization: reclaims disk space after compaction (VACUUM on SQLite,
     * OPTIMIZE TABLE on MySQL). Must run outside a transaction.
     */
    suspend fun vacuumDatabase() {
        val dataSource = compactDataSource ?: return
        val isSQLite = databaseType.equals("SQLite", ignoreCase = true)
        withContext(databaseContext) {
            dataSource.connection.use { connection ->
                connection.autoCommit = true
                connection.createStatement().use { statement ->
                    if (isSQLite) {
                        statement.execute("VACUUM")
                    } else {
                        statement.execute("OPTIMIZE TABLE ${Tables.Actions.tableName}")
                    }
                }
            }
        }
    }

    suspend fun previewActions(params: ActionSearchParams, type: Preview.Type): List<ActionType> = execute {
        when (type) {
            Preview.Type.ROLLBACK -> return@execute selectRollback(params)
            Preview.Type.RESTORE -> return@execute selectRestore(params)
        }
    }

    private fun Transaction.getActionsFromQuery(query: Query): List<ActionType> {
        val actions = mutableListOf<ActionType>()
        val stateRefs = HashMap<ActionType, Int>()
        val oldStateRefs = HashMap<ActionType, Int>()

        val actionIdentifierCache = DatabaseCacheService.actionIdentifierKeys.inverse()
        val worldCache = DatabaseCacheService.worldIdentifierKeys.inverse()
        val objectIdentifierCache = DatabaseCacheService.objectIdentifierKeys.inverse()
        val sourceCache = DatabaseCacheService.sourceKeys.inverse()
        val playerCache = DatabaseCacheService.playerKeys.inverse()
        val playerNameCache = DatabaseCacheService.playernameKeys.inverse()

        for (action in query) {
            val typeSupplier = ActionRegistry.getType(
                actionIdentifierCache[action[Tables.Actions.actionIdentifier].value]!!,
            )
            if (typeSupplier == null) {
                logWarn("Unknown action type ${actionIdentifierCache[action[Tables.Actions.actionIdentifier].value]}")
                continue
            }

            val type = typeSupplier.get()
            type.id = action[Tables.Actions.id].value
            type.timestamp = action[Tables.Actions.timestamp]
            type.pos = BlockPos(action[Tables.Actions.x], action[Tables.Actions.y], action[Tables.Actions.z])
            type.world = worldCache[action[Tables.Actions.world].value]
            type.objectIdentifier = objectIdentifierCache[action[Tables.Actions.objectId].value]!!
            type.oldObjectIdentifier = objectIdentifierCache[action[Tables.Actions.oldObjectId].value]!!
            // Reoptimization: prefer the dictionary ref, fall back to the legacy text
            // column for rows written before the encoding was introduced.
            type.objectState = action[Tables.Actions.blockState]
            type.oldObjectState = action[Tables.Actions.oldBlockState]
            action.getOrNull(Tables.Actions.blockStateRef)?.let { stateRefs[type] = it }
            action.getOrNull(Tables.Actions.oldBlockStateRef)?.let { oldStateRefs[type] = it }
            type.sourceName = sourceCache[action[Tables.Actions.sourceName].value]!!
            type.sourceProfile = action.getOrNull(Tables.Actions.sourcePlayer)?.let {
                NameAndId(playerCache[it.value]!!, playerNameCache[it.value]!!)
            }
            type.extraData = action[Tables.Actions.extraData]
            type.rolledBack = action[Tables.Actions.rolledBack]

            actions.add(type)
        }

        // Resolve dictionary refs in one batch (avoids per-row lookups).
        val allRefs = (stateRefs.values + oldStateRefs.values).toSet()
        if (allRefs.isNotEmpty()) {
            val resolved = resolveBlockStates(allRefs)
            for (action in actions) {
                if (action.objectState == null) {
                    action.objectState = stateRefs[action]?.let { resolved[it] }
                }
                if (action.oldObjectState == null) {
                    action.oldObjectState = oldStateRefs[action]?.let { resolved[it] }
                }
            }
        }

        return actions
    }

    private fun buildQueryParams(params: ActionSearchParams): Op<Boolean> {
        var op: Op<Boolean> = Op.TRUE

        if (params.bounds != null && params.bounds != ActionSearchParams.GLOBAL) {
            op = op.and { Tables.Actions.x.between(params.bounds.minX(), params.bounds.maxX()) }
            op = op.and { Tables.Actions.y.between(params.bounds.minY(), params.bounds.maxY()) }
            op = op.and { Tables.Actions.z.between(params.bounds.minZ(), params.bounds.maxZ()) }
        }

        if (params.before != null && params.after != null) {
            op = op.and {
                Tables.Actions.timestamp.greaterEq(params.after) and Tables.Actions.timestamp.lessEq(params.before)
            }
        } else if (params.before != null) {
            op = op.and { Tables.Actions.timestamp.lessEq(params.before) }
        } else if (params.after != null) {
            op = op.and { Tables.Actions.timestamp.greaterEq(params.after) }
        }

        if (params.rolledBack != null) {
            op = op.and { Tables.Actions.rolledBack.eq(params.rolledBack) }
        }

        op = addParameters(
            op,
            params.sourceNames,
            DatabaseManager::getSourceId,
            Tables.Actions.sourceName,
        )

        op = addParameters(
            op,
            params.actions,
            DatabaseManager::getActionId,
            Tables.Actions.actionIdentifier,
        )

        op = addParameters(
            op,
            params.worlds,
            DatabaseManager::getWorldId,
            Tables.Actions.world,
        )

        op = addParameters(
            op,
            params.objects,
            DatabaseManager::getRegistryKeyId,
            Tables.Actions.objectId,
            Tables.Actions.oldObjectId,
        )

        op = addParameters(
            op,
            params.sourcePlayerIds,
            DatabaseManager::getPlayerId,
            Tables.Actions.sourcePlayer,
        )

        return op
    }

    private fun <E : Comparable<E>, C : EntityID<E>?, T> addParameters(
        op: Op<Boolean>,
        paramSet: Collection<Negatable<T>>?,
        objectToId: Function<T, E?>,
        column: Column<C>,
        orColumn: Column<C>? = null,
    ): Op<Boolean> {
        val idParamSet = mutableSetOf<Negatable<E>>()
        paramSet?.forEach {
            val paramId = objectToId.apply(it.property)
            if (paramId != null) {
                idParamSet.add(Negatable(paramId, it.allowed))
            } else {
                // Unknown source name
                return Op.FALSE
            }
        }
        return addParameters(op, idParamSet, column, orColumn)
    }

    private fun <E : Comparable<E>, C : EntityID<E>?> addParameters(
        op: Op<Boolean>,
        paramSet: Collection<Negatable<E>>?,
        column: Column<C>,
        orColumn: Column<C>? = null,
    ): Op<Boolean> {
        fun addAllowedParameters(allowed: Collection<E>, op: Op<Boolean>): Op<Boolean> {
            if (allowed.isEmpty()) return op

            var operator = if (orColumn != null) {
                column eq allowed.first() or (orColumn eq allowed.first())
            } else {
                column eq allowed.first()
            }

            allowed.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.or { column eq param or (orColumn eq param) }
                } else {
                    operator.or { column eq param }
                }
            }

            return op.and { operator }
        }

        fun addDeniedParameters(denied: Collection<E>, op: Op<Boolean>): Op<Boolean> {
            if (denied.isEmpty()) return op

            var operator = if (orColumn != null) {
                column neq denied.first() and (orColumn neq denied.first())
            } else {
                column neq denied.first() or column.isNull()
            }

            denied.stream().skip(1).forEach { param ->
                operator = if (orColumn != null) {
                    operator.and { column neq param and (orColumn neq param) }
                } else {
                    operator.and { column neq param or column.isNull() }
                }
            }

            return op.and { operator }
        }

        if (paramSet.isNullOrEmpty()) return op

        var newOp = op
        newOp = addAllowedParameters(paramSet.filter { it.allowed }.map { it.property }, newOp)
        newOp = addDeniedParameters(paramSet.filterNot { it.allowed }.map { it.property }, newOp)

        return newOp
    }

    suspend fun logActionBatch(actions: List<ActionType>) {
        execute {
            insertActions(actions)
        }
    }

    suspend fun registerWorld(identifier: Identifier) = execute {
        insertWorld(identifier)
    }

    suspend fun registerActionType(id: String) = execute {
        insertActionType(id)
    }

    suspend fun logPlayer(uuid: UUID, name: String) = execute {
        insertOrUpdatePlayer(uuid, name)
    }

    suspend fun insertIdentifiers(identifiers: Collection<Identifier>) = execute {
        insertRegKeys(identifiers)
    }

    private suspend fun <T : Any?> execute(body: suspend JdbcTransaction.() -> T): T {
        while (Ledger.server.overworld()?.noSave != false) {
            delay(timeMillis = 1000)
        }

        return newSuspendedTransaction(context = databaseContext, db = database) {
            maxAttempts = MAX_QUERY_RETRIES
            minRetryDelay = MIN_RETRY_DELAY
            maxRetryDelay = MAX_RETRY_DELAY

            if (Ledger.config[DatabaseSpec.logSQL]) {
                addLogger(ledgerLogger)
            }
            body(this)
        }
    }

    suspend fun purgeActions(params: ActionSearchParams) {
        execute {
            purgeActions(params)
        }
    }

    suspend fun searchPlayers(players: Set<NameAndId>): List<PlayerResult> = execute {
        return@execute selectPlayers(players)
    }

    private fun Transaction.insertActionType(id: String) {
        Tables.ActionIdentifiers.insertIgnore {
            it[actionIdentifier] = id
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

    private fun Transaction.insertActions(actions: List<ActionType>) {
        val (safe, oversized) = actions.partition {
            it.extraData == null || it.extraData!!.length <= MAX_EXTRA_DATA_BYTES
        }
        oversized.forEach { action ->
            logWarn(
                "Skipping action log: extra_data too large (${action.extraData!!.length} chars) " +
                    "for action ${action.identifier} at " +
                    "[${action.world} ${action.pos.x} ${action.pos.y} ${action.pos.z}] " +
                    "by ${action.sourceProfile?.name ?: action.sourceName}",
            )
        }
        Tables.Actions.batchInsert(safe, shouldReturnGeneratedValues = false) { action ->
            this[Tables.Actions.actionIdentifier] = getOrCreateActionId(action.identifier)
            this[Tables.Actions.timestamp] = action.timestamp
            this[Tables.Actions.x] = action.pos.x
            this[Tables.Actions.y] = action.pos.y
            this[Tables.Actions.z] = action.pos.z
            this[Tables.Actions.objectId] = getOrCreateRegistryKeyId(action.objectIdentifier)
            this[Tables.Actions.oldObjectId] = getOrCreateRegistryKeyId(action.oldObjectIdentifier)
            this[Tables.Actions.world] = getOrCreateWorldId(
                action.world ?: Ledger.server.overworld().dimension()
                    .identifier(),
            )
            // Reoptimization: dictionary-encode block states. Rows carry a nullable int
            // reference into block_states instead of repeating the full state string;
            // the legacy text columns stay null for new writes.
            this[Tables.Actions.blockStateRef] = action.objectState?.let { getOrCreateBlockStateId(it) }
            this[Tables.Actions.oldBlockStateRef] = action.oldObjectState?.let { getOrCreateBlockStateId(it) }
            this[Tables.Actions.sourceName] = getOrCreateSourceId(action.sourceName)
            this[Tables.Actions.sourcePlayer] = action.sourceProfile?.let { getOrCreatePlayerId(it.id) }
            this[Tables.Actions.extraData] = action.extraData
        }
    }

    /**
     * Reoptimization: resolves (or creates) the dictionary id for a block state string.
     */
    private fun getOrCreateBlockStateId(state: String): Int = getOrCreateObjectId(
        state,
        cache.blockStateKeys,
        Tables.BlockState,
        Tables.BlockStates,
        Tables.BlockStates.state,
    )

    /**
     * Reoptimization: batch-resolves block state ids to their strings using the cache,
     * falling back to a single IN (...) query for cache misses.
     */
    private fun Transaction.resolveBlockStates(ids: Set<Int>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val misses = mutableSetOf<Int>()
        val inverse = cache.blockStateKeys.inverse()
        for (id in ids) {
            inverse[id]?.let { result[id] = it } ?: misses.add(id)
        }
        if (misses.isNotEmpty()) {
            Tables.BlockStates.selectAll()
                .where { Tables.BlockStates.id inList misses }
                .forEach { row ->
                    val id = row[Tables.BlockStates.id].value
                    val state = row[Tables.BlockStates.state]
                    result[id] = state
                    cache.blockStateKeys[state] = id
                }
        }
        return result
    }

    private fun Transaction.insertOrUpdatePlayer(uuid: UUID, name: String) {
        val player = Tables.Player.find { Tables.Players.playerId eq uuid }.firstOrNull()

        if (player != null) {
            player.lastJoin = Instant.now()
            player.playerName = name
            cache.playernameKeys.forcePut(name, player.id.value)
        } else {
            val entity = Tables.Player.new {
                this.playerId = uuid
                this.playerName = name
            }
            cache.playerKeys[uuid] = entity.id.value
            cache.playernameKeys.forcePut(name, entity.id.value)
        }
    }

    // Reoptimization: short-lived cache for search result counts, so paging
    // through results does not re-run the full count query on every page.
    private val countCache = java.util.concurrent.ConcurrentHashMap<ActionSearchParams, Pair<Long, Long>>()

    private fun cachedCount(params: ActionSearchParams, recompute: () -> Long): Long {
        val now = System.currentTimeMillis()
        countCache[params]?.let { (count, expiresAt) ->
            if (now < expiresAt) return count
        }
        val count = recompute()
        if (countCache.size > COUNT_CACHE_MAX_ENTRIES) countCache.clear()
        countCache[params] = count to (now + COUNT_CACHE_TTL_MS)
        return count
    }

    private fun Transaction.selectActionsSearch(params: ActionSearchParams, page: Int): SearchResults {
        val actions = mutableListOf<ActionType>()

        var query = Tables.Actions
            .selectAll()
            .andWhere { buildQueryParams(params) }

        // Reoptimization: served from the count cache while paging (TTL 30s)
        val totalActions: Long = cachedCount(params) {
            Tables.Actions
                .selectAll()
                .andWhere { buildQueryParams(params) }
                .count()
        }
        if (totalActions == 0L) return SearchResults(actions, params, page, 0)

        query = query.orderBy(Tables.Actions.id, SortOrder.DESC)
        query = query.limit(config[SearchSpec.pageSize]).offset(
            (config[SearchSpec.pageSize] * (page - 1)).toLong(),
        ) // TODO better pagination without offset - probably doesn't matter as most people stay on first few pages

        actions.addAll(getActionsFromQuery(query))

        val totalPages = ceil(totalActions.toDouble() / config[SearchSpec.pageSize].toDouble()).toInt()

        return SearchResults(actions, params, page, totalPages)
    }

    private fun Transaction.countActions(params: ActionSearchParams): Long = Tables.Actions
        .selectAll()
        .andWhere { buildQueryParams(params) }
        .count()

    private fun Transaction.rollbackActions(actionIds: Set<Int>) {
        Tables.Actions
            .update({ Tables.Actions.id inList actionIds }) {
                it[rolledBack] = true
            }
    }

    private fun Transaction.restoreActions(actionIds: Set<Int>) {
        Tables.Actions
            .update({ Tables.Actions.id inList actionIds }) {
                it[rolledBack] = false
            }
    }

    fun getKnownSources() = cache.sourceKeys.keys

    private fun <T> getObjectId(
        obj: T,
        cache: BiMap<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<T>,
    ): Int? = getObjectId(obj, Function.identity(), cache, table, column)

    private fun <T, S> getObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: BiMap<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<S>,
    ): Int? {
        if (cache.containsKey(obj)) {
            return cache[obj]
        }
        return table.find { column eq mapper.apply(obj) }.firstOrNull()?.id?.value?.also {
            cache.put(obj, it)
        }
    }

    private fun <T> getOrCreateObjectId(
        obj: T,
        cache: BiMap<T, Int>,
        entity: IntEntityClass<*>,
        table: IntIdTable,
        column: Column<T>,
    ): Int = getOrCreateObjectId(obj, Function.identity(), cache, entity, table, column)

    private fun <T, S> getOrCreateObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: BiMap<T, Int>,
        entity: IntEntityClass<*>,
        table: IntIdTable,
        column: Column<S>,
    ): Int {
        getObjectId(obj, mapper, cache, entity, column)?.let { return it }

        return entity[
            table.insertAndGetId {
                it[column] = mapper.apply(obj)
            },
        ].id.value.also { cache.put(obj!!, it) }
    }

    private fun getOrCreatePlayerId(playerId: UUID): Int =
        getOrCreateObjectId(playerId, cache.playerKeys, Tables.Player, Tables.Players, Tables.Players.playerId)

    private fun getOrCreateSourceId(source: String): Int =
        getOrCreateObjectId(source, cache.sourceKeys, Tables.Source, Tables.Sources, Tables.Sources.name)

    private fun getOrCreateActionId(actionTypeId: String): Int = getOrCreateObjectId(
        actionTypeId,
        cache.actionIdentifierKeys,
        Tables.ActionIdentifier,
        Tables.ActionIdentifiers,
        Tables.ActionIdentifiers.actionIdentifier,
    )

    private fun getOrCreateRegistryKeyId(identifier: Identifier): Int = getOrCreateObjectId(
        identifier,
        Identifier::toString,
        cache.objectIdentifierKeys,
        Tables.ObjectIdentifier,
        Tables.ObjectIdentifiers,
        Tables.ObjectIdentifiers.identifier,
    )

    private fun getOrCreateWorldId(identifier: Identifier): Int = getOrCreateObjectId(
        identifier,
        Identifier::toString,
        cache.worldIdentifierKeys,
        Tables.World,
        Tables.Worlds,
        Tables.Worlds.identifier,
    )

    private fun getPlayerId(playerId: UUID): Int? =
        getObjectId(playerId, cache.playerKeys, Tables.Player, Tables.Players.playerId)

    private fun getSourceId(source: String): Int? =
        getObjectId(source, cache.sourceKeys, Tables.Source, Tables.Sources.name)

    private fun getActionId(actionTypeId: String): Int? = getObjectId(
        actionTypeId,
        cache.actionIdentifierKeys,
        Tables.ActionIdentifier,
        Tables.ActionIdentifiers.actionIdentifier,
    )

    private fun getRegistryKeyId(identifier: Identifier): Int? = getObjectId(
        identifier,
        Identifier::toString,
        cache.objectIdentifierKeys,
        Tables.ObjectIdentifier,
        Tables.ObjectIdentifiers.identifier,
    )

    private fun getWorldId(identifier: Identifier): Int? = getObjectId(
        identifier,
        Identifier::toString,
        cache.worldIdentifierKeys,
        Tables.World,
        Tables.Worlds.identifier,
    )

    // Workaround because can't delete from a join in exposed https://kotlinlang.slack.com/archives/C0CG7E0A1/p1605866974117400
    private fun Transaction.purgeActions(params: ActionSearchParams) = Tables.Actions
        .deleteWhere {
            id inSubQuery Tables.Actions.select(id).where(buildQueryParams(params))
        }

    private fun Transaction.selectPlayers(players: Set<NameAndId>): List<PlayerResult> {
        val query = Tables.Players.selectAll()
        for (player in players) {
            query.orWhere { Tables.Players.playerId eq player.id() }
        }

        return Tables.Player.wrapRows(query).toList().map { PlayerResult.fromRow(it) }
    }
}
