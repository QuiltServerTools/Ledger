package com.github.quiltservertools.ledger.api

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.database.DatabaseManager
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

object DatabaseApi {
    @JvmStatic
    fun searchActions(params: ActionSearchParams, page: Int): CompletableFuture<SearchResults> {
        return Ledger.future {
            return@future DatabaseManager.searchActions(params, page)
        }
    }

    @JvmStatic
    fun logAction(action: ActionType) {
        Ledger.launch {
            DatabaseManager.logAction(action)
        }
    }
}
