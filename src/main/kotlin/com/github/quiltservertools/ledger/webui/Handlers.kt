package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Negatable
import io.javalin.http.Context
import io.javalin.http.HttpCode
import kotlinx.coroutines.future.future
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.util.*

object Handlers {

    private const val dashboard_size = 30

    fun handleOverview(ctx: Context) {
        ctx.future(
            Ledger.future {
                val results = DatabaseManager.getDashboardActions(dashboard_size).toSet()
                return@future results.map {
                    SentAction.fromActionType(it)
                }
            }
        )
    }

    fun handlePlayersOverview(ctx: Context) {
        ctx.future(
            Ledger.future {
                val players = DatabaseManager.getDashboardPlayers(dashboard_size).toSet()
                return@future players.map {
                    Account.fromLedgerPlayer(it)
                }
            }
        )
    }

    fun search(ctx: Context) {
        // Iterate through params and add to string like in search command
        val map = ctx.queryParamMap()
        var input = ""

        val page = if (map.containsKey("page")) {
            map["page"]!!.first().toInt()
        } else {
            1
        }
        // Make sure we exclude the page number
        map.filter { (k, _) -> k != "page" }.forEach { (k, v) ->
            v.forEach {
                if (input.isNotEmpty()) {
                    input = input.plus(" ")
                }
                input = input.plus("$k:$it")
            }
        }
        ctx.future(
            Ledger.future {
                val set = mutableSetOf<SentAction>()
                val params = SearchParamArgument.get(input, WebUi.commandSource)

                set.addAll(DatabaseManager.searchActions(params, page).actions.map {
                    SentAction.fromActionType(it)
                })
                return@future set
            }
        )
    }

    fun searchInit(ctx: Context) {
        // Iterate through params and add to string like in search command
        val map = ctx.queryParamMap()
        var input = ""

        // Make sure we exclude the page number
        map.forEach { (k, v) ->
            v.forEach {
                if (input.isNotEmpty()) {
                    input = input.plus(" ")
                }
                input = input.plus("$k:$it")
            }
        }

        ctx.future(
            Ledger.future {
                val params = SearchParamArgument.get(input, WebUi.commandSource)
                return@future DatabaseManager.searchActions(params, 1).pages
            }
        )
    }

    fun showPfp(ctx: Context) {
        val uuid = ctx.sessionAttribute<UUID>("uuid") ?: ctx.status(HttpCode.NOT_FOUND)
        ctx.redirect("https://crafatar.com/avatars/$uuid")
    }

    fun account(ctx: Context) {
        ctx.future(
            Ledger.future {
                val uuid = ctx.sessionAttribute<UUID>("uuid")!!
                val player = DatabaseManager.searchPlayer(uuid)

                return@future Account.fromLedgerPlayer(player)
            }
        )
    }

    fun inspect(ctx: Context) {
        ctx.future(
            Ledger.future {
                return@future getInspectResults(ctx, (ctx.queryParam("page") ?: "1").toInt()).actions.map {
                    SentAction.fromActionType(it)
                }
            }
        )
    }
    fun inspectInit(ctx: Context) {
        ctx.future(
            Ledger.future {
                return@future getInspectResults(ctx, 1).pages
            }
        )
    }

    private suspend fun getInspectResults(ctx: Context, page: Int): SearchResults {
        val pos = BlockPos(
            ctx.queryParam("x")!!.toInt(),
            ctx.queryParam("y")!!.toInt(),
            ctx.queryParam("z")!!.toInt()
        )
        val world = Identifier(ctx.queryParam("world"))
        val params = ActionSearchParams.build {
            min = pos
            max = pos
            this.worlds = mutableSetOf(Negatable.allow(world))
        }
        return DatabaseManager.searchActions(params, page)
    }
}
