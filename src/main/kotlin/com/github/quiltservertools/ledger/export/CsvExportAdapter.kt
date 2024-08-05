package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.AbstractActionType
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.utility.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.text.Text
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

class CsvExportAdapter : AbstractExportAdapter() {
    private fun actionToCsvData(action: AbstractActionType): String {
        val timeStr: String = MessageUtils.instantTimeToFullText(action.timestamp).string
        val sourceName: String = action.getSourceMessage().string
        val actionName: String = action.getActionMessage().string
        val objectName: String = action.getObjectMessage().string
        val x = action.pos.x
        val y = action.pos.y
        val z = action.pos.z
        val worldName: String = action.world?.toString() ?: ""
        val extraData: String = action.extraData ?: ""
        return "$timeStr, $sourceName, $actionName, $objectName, $x, $y, $z, $worldName, $extraData"
    }

    override suspend fun exportFromData(actions: List<ActionType>, exportDir: Path): Path? {
        val time = MessageUtils.instantTimeToFullText(Instant.now(), "yyyy-MM-dd_HH-mm-ss").string
        val exportPath = exportDir.resolve("ledger-export-$time.csv")
        val exportedCsvData = StringBuilder()
        exportedCsvData.append("${Text.translatable("text.ledger.export.csvTitle").string}\n")
        actions.forEach {
            exportedCsvData.append(actionToCsvData(it as AbstractActionType))
            exportedCsvData.append("\n")
        }
        try {
            withContext(Dispatchers.IO) {
                Files.createFile(exportPath)
                Files.writeString(exportPath, exportedCsvData)
            }
            return exportPath
        } catch (_: IOException) {
            return null
        }
    }
}
