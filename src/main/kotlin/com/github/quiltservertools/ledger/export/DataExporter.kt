package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.database.DatabaseManager
import java.nio.file.Path

class DataExporter(private val searchParams: ActionSearchParams, private val exportAdapter: AbstractExportAdapter) {
    private var actionsList: List<ActionType>? = null

    private suspend fun queryFromDatabase(searchParams: ActionSearchParams): List<ActionType> {
        val results = DatabaseManager.searchActions(searchParams, -1)   // search without pagination
        return results.actions
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
