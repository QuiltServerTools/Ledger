package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import io.javalin.http.Context
import kotlinx.coroutines.future.future

object Handlers {
    @SuppressWarnings("MagicNumber")
    fun handleOverview(ctx: Context) {
        ctx.future(
            Ledger.future {
                val results = DatabaseManager.getDashboardActions(12).toSet()
                return@future results.map {
                    SentAction.fromActionType(it)
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
                return@future Length(DatabaseManager.searchActions(params, 1).pages)
            }
        )
    }
}
