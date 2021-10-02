package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.database.DatabaseManager
import io.javalin.http.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

object Handlers {
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressWarnings("MagicNumber")
    fun handleOverview(ctx: Context) {
        ctx.future(
            GlobalScope.future {
                //TODO this is broken for some reason
                return@future DatabaseManager.getDashboardActions(12).toSet()
            }
        )
    }
}
