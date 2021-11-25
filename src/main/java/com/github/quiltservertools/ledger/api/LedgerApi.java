package com.github.quiltservertools.ledger.api;

import com.github.quiltservertools.ledger.actions.ActionType;
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams;
import com.github.quiltservertools.ledger.actionutils.SearchResults;
import java.util.concurrent.CompletableFuture;

public interface LedgerApi {
    CompletableFuture<SearchResults> searchActions(ActionSearchParams params, int page);
    CompletableFuture<Long> countActions(ActionSearchParams params);

    void logAction(ActionType action);
}
