package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.ActionType
import java.io.IOException
import java.nio.file.Path

abstract class AbstractExportAdapter {
    // Start export, return exported file path
    @Throws(IOException::class)
    abstract suspend fun startExport(exportDir: Path): Path?

    // Add a batch of data to export
    @Throws(IOException::class)
    abstract suspend fun addData(actions: List<ActionType>): Boolean

    // End export, do some cleanup here
    @Throws(IOException::class)
    abstract suspend fun endExport(): Boolean
}
