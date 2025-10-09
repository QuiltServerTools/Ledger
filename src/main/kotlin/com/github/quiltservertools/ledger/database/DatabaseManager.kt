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
import com.mojang.authlib.GameProfile
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.v1.core.Column
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

object DatabaseManager {

    // These values are initialised late to allow the database to be created at server start,
    // which means the database file is located in the world folder and allows for per-world databases.
    private lateinit var database: Database
    val databaseType: String
        get() = database.dialect.name

    private val cache = DatabaseCacheService
    private var databaseContext = Dispatchers.IO + CoroutineName("Ledger Database")
    private val ledgerLogger = object : SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            Ledger.logger.info("SQL: ${context.expandArgs(transaction)}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun setup(dataSource: DataSource?) {
        if (dataSource == null) {
            database = Database.connect(getDefaultDatasource())
            databaseContext = newSingleThreadContext("Ledger Database")
        } else {
            database = Database.connect(dataSource)
        }
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
        addLogger(ledgerLogger)
        SchemaUtils.create(
            Tables.Players,
            Tables.Actions,
            Tables.ActionIdentifiers,
            Tables.ObjectIdentifiers,
            Tables.Sources,
            Tables.Worlds,
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
            Tables.Player.all().forEach {
                cache.playerKeys.put(it.playerId, it.id.value)
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

    suspend fun previewActions(
        params: ActionSearchParams,
        type: Preview.Type
    ): List<ActionType> = execute {
        when (type) {
            Preview.Type.ROLLBACK -> return@execute selectRollback(params)
            Preview.Type.RESTORE -> return@execute selectRestore(params)
        }
    }

    private fun getActionsFromQuery(query: Query): List<ActionType> {
        val actions = mutableListOf<ActionType>()

        val actionIdentifierCache = DatabaseCacheService.actionIdentifierKeys.inverse()
        val worldCache = DatabaseCacheService.worldIdentifierKeys.inverse()
        val objectIdentifierCache = DatabaseCacheService.objectIdentifierKeys.inverse()
        val sourceCache = DatabaseCacheService.sourceKeys.inverse()
        val playerCache = DatabaseCacheService.playerKeys.inverse()

        for (action in query) {
            val typeSupplier = ActionRegistry.getType(
                actionIdentifierCache[action[Tables.Actions.actionIdentifier].value]!!
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
            type.objectState = action[Tables.Actions.blockState]
            type.oldObjectState = action[Tables.Actions.oldBlockState]
            type.sourceName = sourceCache[action[Tables.Actions.sourceName].value]!!
            type.sourceProfile = action.getOrNull(Tables.Actions.sourcePlayer)?.let {
                Ledger.server.userCache?.getByUuid(playerCache[it.value]!!)?.orElse(null)
            }
            type.extraData = action[Tables.Actions.extraData]
            type.rolledBack = action[Tables.Actions.rolledBack]

            actions.add(type)
        }

        return actions
    }

    private fun buildQueryParams(params: ActionSearchParams): Op<Boolean> {
        var op: Op<Boolean> = Op.TRUE

        if (params.bounds != null && params.bounds != ActionSearchParams.GLOBAL) {
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

        fun addDeniedParameters(
            denied: Collection<E>,
            op: Op<Boolean>
        ): Op<Boolean> {
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

        var query = Tables.Actions
            .selectAll()
            .andWhere { buildQueryParams(params) }

        val totalActions: Long = countActions(params)
        if (totalActions == 0L) return SearchResults(actions, params, page, 0)

        query = query.orderBy(Tables.Actions.id, SortOrder.DESC)
        query = query.limit(config[SearchSpec.pageSize]).offset(
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

    fun getKnownSources() =
        cache.sourceKeys.keys

    private fun <T> getObjectId(
        obj: T,
        cache: BiMap<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<T>
    ): Int? = getObjectId(obj, Function.identity(), cache, table, column)

    private fun <T, S> getObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: BiMap<T, Int>,
        table: EntityClass<Int, Entity<Int>>,
        column: Column<S>
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
        column: Column<T>
    ): Int =
        getOrCreateObjectId(obj, Function.identity(), cache, entity, table, column)

    private fun <T, S> getOrCreateObjectId(
        obj: T,
        mapper: Function<T, S>,
        cache: BiMap<T, Int>,
        entity: IntEntityClass<*>,
        table: IntIdTable,
        column: Column<S>
    ): Int {
        getObjectId(obj, mapper, cache, entity, column)?.let { return it }

        return entity[
            table.insertAndGetId {
                it[column] = mapper.apply(obj)
            }
        ].id.value.also { cache.put(obj!!, it) }
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
            id inSubQuery Tables.Actions.select(id).where(buildQueryParams(params))
        }

    private fun Transaction.selectPlayers(players: Set<GameProfile>): List<PlayerResult> {
        val query = Tables.Players.selectAll()
        for (player in players) {
            query.orWhere { Tables.Players.playerId eq player.id }
        }

        return Tables.Player.wrapRows(query).toList().map { PlayerResult.fromRow(it) }
    }
}
