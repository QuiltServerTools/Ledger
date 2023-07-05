package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.actionutils.SearchResults
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
import kotlinx.coroutines.delay
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.BlockPos
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
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
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.sqlite.SQLiteDataSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.sql.DataSource
import kotlin.io.path.pathString
import kotlin.math.ceil

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
        val dbFilepath = Ledger.server.getSavePath(WorldSavePath.ROOT).resolve("ledger.sqlite").pathString
        return SQLiteDataSource().apply {
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
            type.objectIdentifier = Identifier(action[Tables.ObjectIdentifiers.identifier])
            type.oldObjectIdentifier = Identifier(
            action[Tables.ObjectIdentifiers.alias("oldObjects")[Tables.ObjectIdentifiers.identifier]]
        )
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

        op = addParameters(
            op,
            params.sourceNames,
            Tables.Sources.name
        )

        op = addParameters(
            op,
            params.actions,
            Tables.ActionIdentifiers.actionIdentifier
        )

        op = addParameters(
            op,
            params.worlds?.map {
                if (it.allowed) {
                    Negatable.allow(it.property.toString())
                } else {
                    Negatable.deny(it.property.toString())
                }
            },
            Tables.Worlds.identifier
        )

        op = addParameters(
            op,
            params.objects?.map {
                if (it.allowed) {
                    Negatable.allow(it.property.toString())
                } else {
                    Negatable.deny(it.property.toString())
                }
            },
            Tables.ObjectIdentifiers.identifier,
            Tables.oldObjectTable[Tables.ObjectIdentifiers.identifier]
        )

        op = addParameters(
            op,
            params.sourcePlayerNames,
            Tables.Players.playerName
        )

        return op
    }

    private fun <E> addParameters(
        op: Op<Boolean>,
        paramSet: Collection<Negatable<E>>?,
        column: Column<E>,
        orColumn: Column<E>? = null
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
            this[Tables.Actions.actionIdentifier] = getActionId(action.identifier)
            this[Tables.Actions.timestamp] = action.timestamp
            this[Tables.Actions.x] = action.pos.x
            this[Tables.Actions.y] = action.pos.y
            this[Tables.Actions.z] = action.pos.z
            this[Tables.Actions.objectId] = getRegistryKeyId(action.objectIdentifier)
            this[Tables.Actions.oldObjectId] = getRegistryKeyId(action.oldObjectIdentifier)
            this[Tables.Actions.world] = getWorldId(action.world ?: Ledger.server.overworld.registryKey.value)
            this[Tables.Actions.blockState] = action.blockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
            this[Tables.Actions.oldBlockState] = action.oldBlockState?.let { NbtUtils.blockStateToProperties(it)?.asString() }
            this[Tables.Actions.sourceName] = getOrCreateSourceId(action.sourceName)
            this[Tables.Actions.sourcePlayer] = action.sourceProfile?.let { selectPlayerId(it.id) }
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
            .innerJoin(Tables.oldObjectTable, { Tables.Actions.oldObjectId }, { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) }

        totalActions = query.copy().count()
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
        .innerJoin(Tables.ActionIdentifiers)
        .innerJoin(Tables.Worlds)
        .leftJoin(Tables.Players)
        .innerJoin(
            Tables.oldObjectTable,
            { Tables.Actions.oldObjectId },
            { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
        .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
        .innerJoin(Tables.Sources)
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
            .innerJoin(Tables.oldObjectTable, { Tables.Actions.oldObjectId }, { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
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
            .innerJoin(Tables.oldObjectTable, { Tables.Actions.oldObjectId }, { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) and (Tables.Actions.rolledBack eq false) }
            .orderBy(Tables.Actions.id, SortOrder.DESC)
        val actionIds = selectQuery.map { it[Tables.Actions.id] }.toSet() // SQLite doesn't support update where so select by ID. Might not be as efficent
        actions.addAll(getActionsFromQuery(selectQuery))

        val updateQuery = Tables.Actions
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
            .innerJoin(Tables.oldObjectTable, { Tables.Actions.oldObjectId }, { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
            .innerJoin(Tables.ObjectIdentifiers, { Tables.Actions.objectId }, { Tables.ObjectIdentifiers.id })
            .innerJoin(Tables.Sources)
            .selectAll()
            .andWhere { buildQueryParams(params) and (Tables.Actions.rolledBack eq true) }
            .orderBy(Tables.Actions.id, SortOrder.ASC)
        val actionIds = selectQuery.map { it[Tables.Actions.id] }.toSet()
        actions.addAll(getActionsFromQuery(selectQuery))

        val updateQuery = Tables.Actions
            .update({ Tables.Actions.id inList actionIds and (Tables.Actions.rolledBack eq true) }) {
                it[rolledBack] = false
            }

        return actions
    }

    private fun Transaction.selectPlayerId(playerId: UUID): Int {
        cache.playerKeys.getIfPresent(playerId)?.let { return it }

        return Tables.Player.find {
            Tables.Players.playerId eq playerId
        }.first().id.value.also { cache.playerKeys.put(playerId, it) }
    }

    private fun Transaction.selectPlayer(playerName: String) =
        Tables.Player.find { Tables.Players.playerName.lowerCase() eq playerName }.firstOrNull()

    private fun Transaction.getOrCreateSourceId(source: String): Int {
        cache.sourceKeys.getIfPresent(source)?.let { return it }

        Tables.Source.find { Tables.Sources.name eq source }.firstOrNull()?.let { return it.id.value }

        return Tables.Source[
            Tables.Sources.insertAndGetId {
            it[name] = source
        }
        ].id.value.also { cache.sourceKeys.put(source, it) }
    }

    private fun Transaction.getActionId(actionTypeId: String): Int {
        cache.actionIdentifierKeys.getIfPresent(actionTypeId)?.let { return it }

        return Tables.ActionIdentifier.find { Tables.ActionIdentifiers.actionIdentifier eq actionTypeId }
            .first().id.value
            .also { cache.actionIdentifierKeys.put(actionTypeId, it) }
    }

    private fun Transaction.getRegistryKeyId(identifier: Identifier): Int {
        cache.objectIdentifierKeys.getIfPresent(identifier)?.let { return it }

        return Tables.ObjectIdentifier.find { Tables.ObjectIdentifiers.identifier eq identifier.toString() }
            .limit(1).first().id.value
            .also { cache.objectIdentifierKeys.put(identifier, it) }
    }

    private fun Transaction.getWorldId(identifier: Identifier): Int {
        cache.worldIdentifierKeys.getIfPresent(identifier)?.let { return it }

        return Tables.World.find { Tables.Worlds.identifier eq identifier.toString() }.limit(1).first().id.value
            .also { cache.worldIdentifierKeys.put(identifier, it) }
    }

    // Workaround because can't delete from a join in exposed https://kotlinlang.slack.com/archives/C0CG7E0A1/p1605866974117400
    private fun Transaction.purgeActions(params: ActionSearchParams) = Tables.Actions
        .deleteWhere {
            Tables.Actions.id inSubQuery Tables.Actions.joinTheTables().slice(Tables.Actions.id)
                .select { buildQueryParams(params) }
        }

    private fun Transaction.selectPlayers(players: Set<GameProfile>): List<PlayerResult> {
        val query = Tables.Players.selectAll()
        for (player in players) {
            query.orWhere { Tables.Players.playerId eq player.id }
        }

        return Tables.Player.wrapRows(query).toList().map { PlayerResult.fromRow(it) }
    }

    private fun Tables.Actions.joinTheTables() = this
        .innerJoin(Tables.ActionIdentifiers)
        .innerJoin(Tables.Worlds)
        .leftJoin(Tables.Players)
        .innerJoin(Tables.oldObjectTable, { oldObjectId }, { Tables.oldObjectTable[Tables.ObjectIdentifiers.id] })
        .innerJoin(Tables.ObjectIdentifiers, { objectId }, { Tables.ObjectIdentifiers.id })
        .innerJoin(Tables.Sources)
}
