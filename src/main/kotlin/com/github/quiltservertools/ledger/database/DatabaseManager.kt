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
import com.google.common.cache.Cache
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.delay
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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

object DatabaseManager {

    // These values are initialised late to allow the database to be created at server start,
    // which means the database file is located in the world folder and allows for per-world databases.
    private lateinit var database: Database
    val databaseType: String
        get() = database.dialect.name

    private val cache = DatabaseCacheService

    fun setup(dataSource: DataSource?) {
        val source = dataSource ?: getDefaultDatasource()
        database = Database.connect(source)
    }

    private fun getDefaultDatasource(): DataSource {
        val dbFilepath = config.getDatabasePath().resolve("ledger.sqlite").pathString
        return SQLiteDataSource(
            SQLiteConfig().apply {
            setJournalMode(SQLiteConfig.JournalMode.WAL)
        }
        ).apply {
            url = "jdbc:sqlite:$dbFilepath"
        }
    }

    fun ensureTables() = transaction {
        addLogger(object : SqlLogger {
            override fun log(context: StatementContext, transaction: Transaction) {
                Ledger.logger.info("SQL: ${context.expandArgs(transaction)}")
            }
        })
        SchemaUtils.createMissingTablesAndColumns(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.ObjectIdentifiers,
            Tables.Sources,
            Tables.Worlds,
            withLogs = true
        )
        logInfo("Tables created")
    }

    suspend fun setupCache() {
        execute {
            Tables.ActionIdentifier.all().forEach {
                cache.actionIdentifierKeys.put(it.identifier, it.id.value)
            }
            Tables.World.all().forEach {
                cache.worldIdentifierKeys.put(it.identifier, it.id.value)
            }
            Tables.ObjectIdentifier.all().forEach {
                cache.objectIdentifierKeys.put(it.identifier, it.id.value)
            }
            Tables.Source.all().forEach {
                cache.sourceKeys.put(it.name, it.id.value)
            }
        }
    }

    suspend fun autoPurge() {
        if (config[DatabaseSpec.autoPurgeDays] > 0) {
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

    suspend fun searchActions(params: ActionSearchParams, page: Int): SearchResults = execute {
        return@execute selectActionsSearch(params, page)
    }

    suspend fun countActions(params: ActionSearchParams): Long = execute {
        return@execute countActions(params)
    }

    suspend fun rollbackActions(params: ActionSearchParams): List<ActionType> = execute {
        return@execute selectAndRollbackActions(params)
    }

    suspend fun restoreActions(params: ActionSearchParams): List<ActionType> = execute {
        return@execute selectAndRestoreActions(params)
    }

    suspend fun previewActions(
        params: ActionSearchParams,
        type: Preview.Type
    ): List<ActionType> = execute {
        return@execute selectActionsPreview(params, type)
    }

    private fun getActionsFromQuery(query: Query): List<ActionType> {
        val actions = mutableListOf<ActionType>()

        for (action in query) {
            val typeSupplier = ActionRegistry.getType(action[Tables.ActionIdentifiers.actionIdentifier])
            if (typeSupplier == null) {
                logWarn("Unknown action type ${action[Tables.ActionIdentifiers.actionIdentifier]}")
                continue
            }

            val type = typeSupplier.get()
            type.timestamp = action[Tables.Actions.timestamp]
            type.pos = BlockPos(action[Tables.Actions.x], action[Tables.Actions.y], action[Tables.Actions.z])
            type.world = Identifier.tryParse(action[Tables.Worlds.identifier])
            type.objectIdentifier = Identifier.of(action[Tables.ObjectIdentifiers.identifier])
            type.oldObjectIdentifier = Identifier.of(
                action[Tables.ObjectIdentifiers.alias("oldObjects")[Tables.ObjectIdentifiers.identifier]]
            )
            type.objectState = action[Tables.Actions.blockState]
            type.oldObjectState = action[Tables.Actions.oldBlockState]
            type.sourceName = action[Tables.Sources.name]
            type.sourceProfile = action.getOrNull(Tables.Players.playerId)?.let {
                GameProfile(it, action[Tables.Players.playerName])
            }
            type.extraData = action[Tables.Actions.extraData]
            type.rolledBack = action[Tables.Actions.rolledBack]

            actions.add(type)
        }

        return actions
    }

    private fun buildQueryParams(params: ActionSearchParams): Op<Boolean> {
        var op: Op<Boolean> = Op.TRUE

        if (params.bounds != null) {
            op = op.and { Tables.Actions.x.between(params.bounds.minX, params.bounds.maxX) }
            op = op.and { Tables.Actions.y.between(params.bounds.minY, params.bounds.maxY) }
            op = op.and { Tables.Actions.z.between(params.bounds.minZ, params.bounds.maxZ) }
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
            Tables.Actions.sourceName
        )

        op = addParameters(
            op,
            params.actions,
            DatabaseManager::getActionId,
            Tables.Actions.actionIdentifier
        )

        op = addParameters(
            op,
            params.worlds,
            DatabaseManager::getWorldId,
            Tables.Actions.world
        )

        op = addParameters(
            op,
            params.objects,
            DatabaseManager::getRegistryKeyId,
            Tables.Actions.objectId,
            Tables.Actions.oldObjectId
        )

        op = addParameters(
            op,
            params.sourcePlayerIds,
            DatabaseManager::getPlayerId,
            Tables.Actions.sourcePlayer
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
        fun addAllowedParameters(
            allowed: Collection<E>,
            op: Op<Boolean>
        ): Op<Boolean> {
            if (allowed.isEmpty()) return op

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

            return op.and { operator }
        }

        fun addDeniedParameters(
            denied: Collection<E>,
            op: Op<Boolean>
        ): Op<Boolean> {
            if (denied.isEmpty()) return op

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
            insertOrUpdatePlayer(uuid, name)
        }

    suspend fun insertIdentifiers(identifiers: Collection<Identifier>) =
        execute {
            insertRegKeys(identifiers)
        }

    private suspend fun <T : Any?> execute(body: suspend Transaction.() -> T): T {
        while (Ledger.server.overworld?.savingDisabled != false) {
            delay(timeMillis = 1000)
        }

        return newSuspendedTransaction(db = database) {
            repetitionAttempts = MAX_QUERY_RETRIES
            minRepetitionDelay = MIN_RETRY_DELAY
            maxRepetitionDelay = MAX_RETRY_DELAY

            if (Ledger.config[DatabaseSpec.logSQL]) {
                addLogger(object : SqlLogger {
                    override fun log(context: StatementContext, transaction: Transaction) {
                        Ledger.logger.info("SQL: ${context.expandArgs(transaction)}")
                    }
                })
            }
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
        Tables.Actions.batchInsert(actions, shouldReturnGeneratedValues = false) { action ->
            this[Tables.Actions.actionIdentifier] = getOrCreateActionId(action.identifier)
            this[Tables.Actions.timestamp] = action.timestamp
            this[Tables.Actions.x] = action.pos.x
            this[Tables.Actions.y] = action.pos.y
            this[Tables.Actions.z] = action.pos.z
            this[Tables.Actions.objectId] = getOrCreateRegistryKeyId(action.objectIdentifier)
            this[Tables.Actions.oldObjectId] = getOrCreateRegistryKeyId(action.oldObjectIdentifier)
            this[Tables.Actions.world] = getOrCreateWorldId(action.world ?: Ledger.server.overworld.registryKey.value)
            this[Tables.Actions.blockState] = action.objectState
            this[Tables.Actions.oldBlockState] = action.oldObjectState
            this[Tables.Actions.sourceName] = getOrCreateSourceId(action.sourceName)
            this[Tables.Actions.sourcePlayer] = action.sourceProfile?.let { getOrCreatePlayerId(it.id) }
            this[Tables.Actions.extraData] = action.extraData
        }
    }

    private fun Transaction.insertOrUpdatePlayer(uuid: UUID, name: String) {
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
        val actions = mutableListOf<ActionType>()
        var totalActions: Long

        var query = Tables.Actions
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .leftJoin(Tables.Players)
            .innerJoin(
                Tables.oldObjectTable,
                { Tables.Actions.oldObjectId },
                { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] }
            )
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) }

        totalActions = countActions(params)
        if (totalActions == 0L) return SearchResults(actions, params, page, 0)

        query = query.orderBy(Tables.Actions.id, SortOrder.DESC)
        query = query.limit(
            config[SearchSpec.pageSize],
            (config[SearchSpec.pageSize] * (page - 1)).toLong()
        ) // TODO better pagination without offset - probably doesn't matter as most people stay on first few pages

        actions.addAll(getActionsFromQuery(query))

        val totalPages = ceil(totalActions.toDouble() / config[SearchSpec.pageSize].toDouble()).toInt()

        return SearchResults(actions, params, page, totalPages)
    }

    private fun Transaction.countActions(params: ActionSearchParams): Long = Tables.Actions
        .selectAll()
        .andWhere { buildQueryParams(params) }
        .count()

    private fun Transaction.selectActionsPreview(
        params: ActionSearchParams,
        type: Preview.Type
    ): MutableList<ActionType> {
        val actions = mutableListOf<ActionType>()

        val isRestore = type == Preview.Type.RESTORE

        val selectQuery = Tables.Actions
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .leftJoin(Tables.Players)
            .innerJoin(
                Tables.oldObjectTable,
                { Tables.Actions.oldObjectId },
                { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] }
            )
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) and (Tables.Actions.rolledBack eq isRestore) }
            .orderBy(Tables.Actions.id, if (isRestore) SortOrder.ASC else SortOrder.DESC)
        actions.addAll(getActionsFromQuery(selectQuery))

        return actions
    }

    private fun Transaction.selectAndRollbackActions(params: ActionSearchParams): MutableList<ActionType> {
        val actions = mutableListOf<ActionType>()

        val selectQuery = Tables.Actions
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .leftJoin(Tables.Players)
            .innerJoin(
                Tables.oldObjectTable,
                { Tables.Actions.oldObjectId },
                { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] }
            )
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) and (Tables.Actions.rolledBack eq false) }
            .orderBy(Tables.Actions.id, SortOrder.DESC)
        val actionIds = selectQuery.map { it[Tables.Actions.id] }
            .toSet() // SQLite doesn't support update where so select by ID. Might not be as efficent
        actions.addAll(getActionsFromQuery(selectQuery))

        Tables.Actions
            .update({ Tables.Actions.id inList actionIds and (Tables.Actions.rolledBack eq false) }) {
                it[rolledBack] = true
            }

        return actions
    }

    private fun Transaction.selectAndRestoreActions(params: ActionSearchParams): MutableList<ActionType> {
        val actions = mutableListOf<ActionType>()

        val selectQuery = Tables.Actions
            .innerJoin(Tables.ActionIdentifiers)
            .innerJoin(Tables.Worlds)
            .leftJoin(Tables.Players)
            .innerJoin(
                Tables.oldObjectTable,
                { Tables.Actions.oldObjectId },
                { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] }
            )
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) and (Tables.Actions.rolledBack eq true) }
            .orderBy(Tables.Actions.id, SortOrder.ASC)
        val actionIds = selectQuery.map { it[Tables.Actions.id] }.toSet()
        actions.addAll(getActionsFromQuery(selectQuery))

        Tables.Actions
            .update({ Tables.Actions.id inList actionIds and (Tables.Actions.rolledBack eq true) }) {
                it[rolledBack] = false
            }

        return actions
    }

    fun getKnownSources() =
        cache.sourceKeys.asMap().keys

    private fun <T> getObjectId(
        obj: T,
        cache: Cache<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<T>
    ): Int? = getObjectId(obj, Function.identity(), cache, table, column)

    private fun <T, S> getObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: Cache<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<S>
    ): Int? {
        cache.getIfPresent(obj)?.let { return it }
        return table.find { column eq mapper.apply(obj) }.firstOrNull()?.id?.value?.also {
            cache.put(obj, it)
        }
    }

    private fun <T> getOrCreateObjectId(
        obj: T,
        cache: Cache<T, Int>,
        entity: IntEntityClass<*>,
        table: IntIdTable,
        column: Column<T>
    ): Int =
        getOrCreateObjectId(obj, Function.identity(), cache, entity, table, column)

    private fun <T, S> getOrCreateObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: Cache<T, Int>,
        entity: IntEntityClass<*>,
        table: IntIdTable,
        column: Column<S>
    ): Int {
        getObjectId(obj, mapper, cache, entity, column)?.let { return it }

        return entity[
            table.insertAndGetId {
                it[column] = mapper.apply(obj)
            }
        ].id.value.also { cache.put(obj, it) }
    }

    private fun getOrCreatePlayerId(playerId: UUID): Int =
        getOrCreateObjectId(playerId, cache.playerKeys, Tables.Player, Tables.Players, Tables.Players.playerId)

    private fun getOrCreateSourceId(source: String): Int =
        getOrCreateObjectId(source, cache.sourceKeys, Tables.Source, Tables.Sources, Tables.Sources.name)

    private fun getOrCreateActionId(actionTypeId: String): Int =
        getOrCreateObjectId(
            actionTypeId,
            cache.actionIdentifierKeys,
            Tables.ActionIdentifier,
            Tables.ActionIdentifiers,
            Tables.ActionIdentifiers.actionIdentifier
        )

    private fun getOrCreateRegistryKeyId(identifier: Identifier): Int =
        getOrCreateObjectId(
            identifier,
            Identifier::toString,
            cache.objectIdentifierKeys,
            Tables.ObjectIdentifier,
            Tables.ObjectIdentifiers,
            Tables.ObjectIdentifiers.identifier
        )

    private fun getOrCreateWorldId(identifier: Identifier): Int =
        getOrCreateObjectId(
            identifier,
            Identifier::toString,
            cache.worldIdentifierKeys,
            Tables.World,
            Tables.Worlds,
            Tables.Worlds.identifier
        )

    private fun getPlayerId(playerId: UUID): Int? =
        getObjectId(playerId, cache.playerKeys, Tables.Player, Tables.Players.playerId)

    private fun getSourceId(source: String): Int? =
        getObjectId(source, cache.sourceKeys, Tables.Source, Tables.Sources.name)

    private fun getActionId(actionTypeId: String): Int? =
        getObjectId(
            actionTypeId,
            cache.actionIdentifierKeys,
            Tables.ActionIdentifier,
            Tables.ActionIdentifiers.actionIdentifier
        )

    private fun getRegistryKeyId(identifier: Identifier): Int? =
        getObjectId(
            identifier,
            Identifier::toString,
            cache.objectIdentifierKeys,
            Tables.ObjectIdentifier,
            Tables.ObjectIdentifiers.identifier
        )

    private fun getWorldId(identifier: Identifier): Int? =
        getObjectId(
            identifier,
            Identifier::toString,
            cache.worldIdentifierKeys,
            Tables.World,
            Tables.Worlds.identifier
        )

    // Workaround because can't delete from a join in exposed https://kotlinlang.slack.com/archives/C0CG7E0A1/p1605866974117400
    private fun Transaction.purgeActions(params: ActionSearchParams) = Tables.Actions
        .deleteWhere {
            Tables.Actions.id inSubQuery Tables.Actions.select(Tables.Actions.id)
                .where(buildQueryParams(params))
        }

    private fun Transaction.selectPlayers(players: Set<GameProfile>): List<PlayerResult> {
        val query = Tables.Players.selectAll()
        for (player in players) {
            query.orWhere { Tables.Players.playerId eq player.id }
        }

        return Tables.Player.wrapRows(query).toList().map { PlayerResult.fromRow(it) }
    }
}
