package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.database.DatabaseManager
import java.nio.file.Path

class DataExporter(private val searchParams: ActionSearchParams, private val exportAdapter: AbstractExportAdapter) {
    var actionsList: List<ActionType>? = null

    suspend fun queryFromDatabase(searchParams: ActionSearchParams): List<ActionType> {
        var results = DatabaseManager.searchActions(searchParams, 0)
        val actions = arrayListOf<ActionType>()
        for (i in results.page..results.pages) {
            results = DatabaseManager.searchActions(searchParams, i)
            results.actions.forEach {
                actions.add(it)
            }
        }
        return actions
    }

    suspend fun getExportDataCount(): Int {
        if (actionsList == null) {
            actionsList = queryFromDatabase(searchParams)
        }
        return actionsList!!.size
    }

    suspend fun exportTo(exportDir: Path): Path? {
        if (actionsList == null) {
            actionsList = queryFromDatabase(searchParams)
        }
        exportDir.toFile().mkdirs()
        return exportAdapter.exportFromData(actionsList!!.toList(), exportDir)
    }
}
