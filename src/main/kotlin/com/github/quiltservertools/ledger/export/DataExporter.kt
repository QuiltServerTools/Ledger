package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.config.ExportSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.TextColorPallet
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.io.IOException
import java.nio.file.Path
import kotlin.math.ceil

class DataExporter(
    private val searchParams: ActionSearchParams,
    private val exportAdapter: AbstractExportAdapter,
    private val requestSource: ServerCommandSource? = null
) {
    private suspend fun getExportDataCount(): Long = DatabaseManager.countActions(searchParams)

    @Throws(IOException::class)
    suspend fun exportTo(exportDir: Path): Path? {
        exportDir.toFile().mkdirs()
        val exportedPath = exportAdapter.startExport(exportDir)

        val totalDataCount = getExportDataCount()
        requestSource?.sendFeedback({
            Text.translatable("text.ledger.export.actions", totalDataCount).setStyle(TextColorPallet.secondary)
        }, false)

        val pageSize = config[ExportSpec.batchSize]
        val pagesCount = ceil(totalDataCount.toDouble() / pageSize.toDouble()).toInt()
        for (i in 1..pagesCount) {
            requestSource?.sendFeedback({
                Text.translatable("text.ledger.export.exporting", i, pagesCount).setStyle(TextColorPallet.secondary)
            }, false)
            val result = DatabaseManager.searchActions(searchParams, i, pageSize)
            exportAdapter.addData(result.actions)
        }

        exportAdapter.endExport()
        return exportedPath
    }
}
