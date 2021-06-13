package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.ActionType

data class SearchResults(
    val actions: List<ActionType>,
    val searchParams: ActionSearchParams,
    val page: Int,
    val pages: Int
)
