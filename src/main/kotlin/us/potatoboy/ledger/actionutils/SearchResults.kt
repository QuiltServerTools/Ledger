package us.potatoboy.ledger.actionutils

import us.potatoboy.ledger.actions.ActionType
import kotlin.math.ceil

data class SearchResults(val actions: List<ActionType>, val searchParams: ActionSearchParams, val page: Int, val pages: Int)