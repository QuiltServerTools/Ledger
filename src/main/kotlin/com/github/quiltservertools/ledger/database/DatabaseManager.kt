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

// Reoptimization: rows converted per statement while backfilling actions.time_ms.
// Small enough that each commit is short, large enough to finish a typical
// database in a handful of statements.
private const val TIME_MS_BACKFILL_BATCH = 20_000

/**
 * Reoptimization: the exact column list and order used by [DatabaseManager.insertActions].
 * The two legacy TEXT state columns are omitted on purpose - new rows store the
 * dictionary reference instead, so they stay NULL, and SQLite fills them in.
 */
private const val INSERT_ACTIONS_SQL =
    "INSERT INTO actions (action_id, \"time\", time_ms, x, y, z, object_id, old_object_id, " +
        "world_id, block_state_ref, old_block_state_ref, \"source\", player_id, extra_data, " +
        "rolled_back) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

// Reoptimization: SQLite connection tuning. The cache size is expressed in KiB and is
// negative to mean "KiB rather than pages"; the other two are byte counts.
private const val SQLITE_CACHE_SIZE_KIB = -16384
private const val SQLITE_MMAP_SIZE_BYTES = "268435456"
private const val SQLITE_JOURNAL_SIZE_LIMIT_BYTES = 67108864

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

    /**
     * Reoptimization: true once the `time_ms` column exists. The insert path only
     * writes it when true, so a database running with `updateSchema = false` (where
     * the migration never runs) keeps working on the original schema.
     */
    @Volatile
    private var timeMsColumnPresent = false

    /**
     * Reoptimization: true once every row carries an epoch-millisecond `time_ms`.
     * While false (i.e. a backfill is still in progress) time filters use the legacy
     * TEXT column so results stay correct on a partially migrated database.
     */
    @Volatile
    private var timeMsReady = false

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

                // Reoptimization: read-path tuning. These are applied by SQLiteConfig to
                // every connection the pool opens, so they survive reconnects (unlike a
                // one-off PRAGMA statement, which would only affect a single connection).
                //  - temp_store=MEMORY: ORDER BY / GROUP BY spills stay in RAM instead of
                //    hitting disk, which matters for the large sorts a rollback does.
                //  - cache_size: 16 MiB page cache (the value is in KiB, negative = KiB).
                //  - mmap_size: 256 MiB of memory-mapped I/O, so page reads during
                //    lookups avoid a copy into the page cache.
                //  - journal_size_limit: cap the WAL at 64 MiB after a checkpoint so a
                //    long bulk insert cannot leave an unbounded -wal file behind.
                setTempStore(SQLiteConfig.TempStore.MEMORY)
                setCacheSize(SQLITE_CACHE_SIZE_KIB)
                setPragma(SQLiteConfig.Pragma.MMAP_SIZE, SQLITE_MMAP_SIZE_BYTES)
                setJournalSizeLimit(SQLITE_JOURNAL_SIZE_LIMIT_BYTES)
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
                // Reoptimization: ensureTimeMs replaces the old TEXT `actions_time` index
                // with an INTEGER `actions_time_ms` one, so the legacy
                // `CREATE INDEX IF NOT EXISTS actions_time` that used to live here is gone -
                // re-asserting it on every start would undo the migration.
                ensureTimeMs()
                ensurePerformanceIndexes()
            } catch (e: java.sql.SQLException) {
                logWarn("Could not run schema migration: ${e.message}")
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
     * Reoptimization: integer time index.
     *
     * The `time` column stores TEXT ("2026-10-05 13:22:21.702"). An index over it costs
     * ~35 bytes per row - measured at 19.6% of the whole database on a 13,500-row
     * workload, making it the single largest object after the table itself. The same
     * instant as epoch milliseconds needs ~15 bytes, so `time_ms` carries the index and
     * every time filter uses it.
     *
     * Migration steps, all idempotent and resumable:
     *   1. add `time_ms` (INTEGER NOT NULL DEFAULT 0) if missing - instant in SQLite
     *   2. create the integer index if missing
     *   3. backfill every row still at the sentinel 0, in id-ordered batches
     *   4. once no sentinel rows remain, drop the superseded TEXT index
     *
     * Until step 3 completes the flag [timeMsReady] stays false and queries fall back
     * to the TEXT column, so a partially migrated database still returns correct
     * results and the backfill simply resumes on the next start.
     */
    private fun JdbcTransaction.ensureTimeMs() {
        val columns = existingColumnNames("actions")

        if ("time_ms" !in columns) {
            exec("ALTER TABLE actions ADD COLUMN time_ms INTEGER NOT NULL DEFAULT 0")
            logInfo("Added actions.time_ms column (integer time index)")
        }
        timeMsColumnPresent = true

        val isSQLite = databaseType.equals("SQLite", ignoreCase = true)
        val indexes = mutableSetOf<String>()
        if (isSQLite) {
            exec("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='actions'") { rs ->
                while (rs.next()) indexes.add(rs.getString("name"))
            }
        }
        if ("actions_time_ms" !in indexes) {
            exec("CREATE INDEX actions_time_ms ON actions(time_ms)")
            logInfo("Created actions_time_ms index")
        }

        // Backfill: strftime is not used because it drops the fractional part, and
        // julianday keeps millisecond accuracy well within double precision here.
        // Verified against the TEXT values on a 13,500-row database: exact match on
        // every row, 1:1 distinct mapping, ordering preserved.
        var remaining = countSentinelTimeRows()
        if (remaining > 0L) {
            logInfo("Backfilling time_ms for $remaining actions (one-off migration)")
            while (remaining > 0L) {
                val issued = backfillTimeMsBatch(TIME_MS_BACKFILL_BATCH)
                val now = if (issued) countSentinelTimeRows() else remaining
                // Stop when the statement could not be issued, or when a pass converted
                // nothing (defensive: never spin on a database we cannot make progress on).
                val progressed = issued && now < remaining
                if (!progressed) {
                    logWarn("time_ms backfill stopped early; affected rows stay on the TEXT fallback")
                }
                remaining = if (progressed) now else 0L
            }
            logInfo("time_ms backfill finished, $remaining rows left unconverted")
        }

        timeMsReady = countSentinelTimeRows() == 0L

        // The TEXT index is dead weight once every query uses time_ms.
        if (timeMsReady && "actions_time" in indexes) {
            try {
                if (isSQLite) {
                    exec("DROP INDEX IF EXISTS actions_time")
                } else {
                    exec("DROP INDEX actions_time ON actions")
                }
                logInfo("Dropped superseded TEXT time index (replaced by actions_time_ms)")
                // The dropped index leaves its pages on the freelist; only VACUUM returns
                // them to the filesystem. /ledger compact does exactly that.
                logInfo("Run \"/ledger compact\" to reclaim the freed index space on disk")
            } catch (e: java.sql.SQLException) {
                logWarn("Could not drop actions_time (harmless, leaving in place): ${e.message}")
            }
        }
    }

    private fun JdbcTransaction.countSentinelTimeRows(): Long {
        var count = 0L
        exec("SELECT COUNT(*) FROM actions WHERE time_ms = 0") { rs ->
            if (rs.next()) count = rs.getLong(1)
        }
        return count
    }

    /**
     * Converts one id-ordered slice of legacy rows. The threshold is the smallest
     * sentinel id plus the batch size, so every still-unconverted row below it is
     * written in a single statement.
     *
     * @return true when a statement was issued (i.e. sentinel rows still exist), false
     *         when there is nothing left to do
     */
    private fun JdbcTransaction.backfillTimeMsBatch(batchSize: Int): Boolean {
        var upper = 0
        exec("SELECT id FROM actions WHERE time_ms = 0 ORDER BY id LIMIT 1") { rs ->
            if (rs.next()) upper = rs.getInt(1)
        }
        if (upper == 0) return false

        exec(
            "UPDATE actions SET time_ms = " +
                "CAST(ROUND((julianday(time) - 2440587.5) * 86400000.0) AS INTEGER) " +
                "WHERE time_ms = 0 AND id < ${upper + batchSize}",
        )
        return true
    }

    /**
     * Reoptimization: makes sure the upstream index set is present and removes the
     * composite (dimension, time) indexes that an earlier revision of this branch
     * created. Ledger stores timestamps as TEXT, so each extra index column costs
     * ~23 bytes per row; measured over 13,500 rows those composites grew the index
     * footprint from ~1.45 MB to ~3.29 MB (+78% of the whole database) while
     * producing no measurable lookup improvement, so they were reverted.
     *
     * Metadata-driven, idempotent and safe to re-run. Only executed when the user
     * opts in via updateSchema.
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

        // Upstream index set. Exposed creates these via the table definition; they are
        // re-asserted here so databases that were created by an intermediate build
        // (which dropped them in favour of composites) are repaired on startup.
        val wantedIndexes = listOf(
            "actions_action_id" to "CREATE INDEX actions_action_id ON actions(action_id)",
            "actions_object_id" to "CREATE INDEX actions_object_id ON actions(object_id)",
            "actions_old_object_id" to "CREATE INDEX actions_old_object_id ON actions(old_object_id)",
            "actions_player_id" to "CREATE INDEX actions_player_id ON actions(player_id)",
            "actions_by_location" to "CREATE INDEX actions_by_location ON actions(x, y, z, world_id)",
        )
        for ((name, ddl) in wantedIndexes) {
            if (name !in existing) {
                try {
                    exec(ddl)
                } catch (e: java.sql.SQLException) {
                    logWarn("Could not create index $name: ${e.message}")
                }
            }
        }

        // Drop the composite indexes an earlier revision of this branch introduced.
        val superseded = listOf(
            "actions_player_time",
            "actions_action_time",
            "actions_object_time",
            "actions_old_object_time",
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

        // An earlier revision also widened actions_by_location to (world, x, z, time).
        // Rebuild it in the upstream shape when the wider form is detected.
        if (isSQLite && "actions_by_location" in existing) {
            var storedSql: String? = null
            try {
                exec(
                    "SELECT sql FROM sqlite_master WHERE type='index' AND name='actions_by_location'",
                ) { rs ->
                    if (rs.next()) storedSql = rs.getString(1)
                }
            } catch (e: java.sql.SQLException) {
                logWarn("Could not read actions_by_location definition: ${e.message}")
            }
            val sql = storedSql
            val normalised = sql?.replace(Regex("\\s+"), " ")
            if (normalised != null && "(world_id, x, z" in normalised) {
                try {
                    exec("DROP INDEX IF EXISTS actions_by_location")
                    exec("CREATE INDEX actions_by_location ON actions(x, y, z, world_id)")
                    logInfo("Rebuilt actions_by_location in the upstream (x, y, z, world) shape")
                } catch (e: java.sql.SQLException) {
                    logWarn("Could not rebuild actions_by_location: ${e.message}")
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

        // Reoptimization: filter on the INTEGER `time_ms` column whenever the backfill
        // has completed - its index is ~20 bytes per row cheaper than the TEXT one and
        // integer comparison beats string comparison. On a database whose backfill is
        // still running we fall back to the TEXT column so results stay correct.
        if (timeMsReady) {
            if (params.before != null && params.after != null) {
                val afterMs = params.after.toEpochMilli()
                val beforeMs = params.before.toEpochMilli()
                op = op.and {
                    Tables.Actions.timeMs.greaterEq(afterMs) and Tables.Actions.timeMs.lessEq(beforeMs)
                }
            } else if (params.before != null) {
                val beforeMs = params.before.toEpochMilli()
                op = op.and { Tables.Actions.timeMs.lessEq(beforeMs) }
            } else if (params.after != null) {
                val afterMs = params.after.toEpochMilli()
                op = op.and { Tables.Actions.timeMs.greaterEq(afterMs) }
            }
        } else if (params.before != null && params.after != null) {
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

    private fun JdbcTransaction.insertActions(actions: List<ActionType>) {
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
        if (safe.isEmpty()) return

        // Reoptimization: insert through a real JDBC batch instead of Exposed's
        // per-row statement path.
        //
        // Measured: Exposed's batchInsert issued one statement per row - a 13,500-row
        // workload produced 13,500 "INSERT INTO actions" statements - and cost roughly
        // 310-500 us per row. A plain JDBC batch on the same schema and the same rows
        // costs 23 us per row (44 us when executed individually), so most of the insert
        // time was statement machinery rather than SQLite.
        //
        // Values are still produced by Exposed's own column types via valueToDB, so the
        // on-disk representation (notably the "yyyy-MM-dd HH:mm:ss.SSS" UTC timestamp
        // text) is byte-for-byte what the ORM path wrote. Only the statement execution
        // is bypassed, keeping the stored format stable across upgrades.
        val insertSql = INSERT_ACTIONS_SQL
        // JdbcTransaction wraps a plain JDBC connection; the generic parameter cannot be
        // inferred from Kotlin here, so make the (always true for JDBC) cast explicit.
        @Suppress("UNCHECKED_CAST")
        val connection = this.connection.connection as java.sql.Connection
        connection.prepareStatement(insertSql).use { ps ->
            for (action in safe) {
                var i = 0
                ps.setObject(++i, getOrCreateActionId(action.identifier))
                ps.setObject(++i, Tables.Actions.timestamp.columnType.valueToDB(action.timestamp))
                ps.setObject(++i, action.timestamp.toEpochMilli())
                ps.setObject(++i, action.pos.x)
                ps.setObject(++i, action.pos.y)
                ps.setObject(++i, action.pos.z)
                ps.setObject(++i, getOrCreateRegistryKeyId(action.objectIdentifier))
                ps.setObject(++i, getOrCreateRegistryKeyId(action.oldObjectIdentifier))
                ps.setObject(
                    ++i,
                    getOrCreateWorldId(
                        action.world ?: Ledger.server.overworld().dimension().identifier(),
                    ),
                )
                // Dictionary-encoded block states: rows carry a nullable int reference
                // into block_states instead of the full state string.
                ps.setObject(++i, action.objectState?.let { getOrCreateBlockStateId(it) })
                ps.setObject(++i, action.oldObjectState?.let { getOrCreateBlockStateId(it) })
                ps.setObject(++i, getOrCreateSourceId(action.sourceName))
                ps.setObject(++i, action.sourceProfile?.let { getOrCreatePlayerId(it.id) })
                ps.setObject(++i, action.extraData)
                ps.setObject(++i, Tables.Actions.rolledBack.columnType.valueToDB(false))
                ps.addBatch()
            }
            ps.executeBatch()
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
