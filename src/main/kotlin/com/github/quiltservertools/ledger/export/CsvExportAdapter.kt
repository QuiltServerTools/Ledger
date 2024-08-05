package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.AbstractActionType
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.text.Text
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class CsvExportAdapter : AbstractExportAdapter() {
    private fun actionToCsvData(action: AbstractActionType): Text {
        val timeStr: String = MessageUtils.instantTimeToFullText(action.timestamp).string
        val sourceName: String = action.getSourceMessage().string
        val actionName: String = action.getActionMessage().string
        val objectName: String = action.getObjectMessage().string
        val x = action.pos.x
        val y = action.pos.y
        val z = action.pos.z
        val worldName: String = action.world?.let { "$it" } ?: ""
        return "$timeStr, $sourceName, $actionName, $objectName, $x, $y, $z, $worldName".literal()
    }

    override suspend fun exportFromData(actions: List<ActionType>, exportDir: Path): Path? {
        val time = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date.from(Instant.now()))
        val exportPath = exportDir.resolve("ledger-export-$time.csv")
        val exportedCsvData = StringBuilder()
        exportedCsvData.append("${Text.translatable("text.ledger.export.csvTitle").string}\n")
        actions.forEach {
            exportedCsvData.append("${actionToCsvData(it as AbstractActionType).string}\n")
        }
        return try {
            Files.createFile(exportPath)
            Files.writeString(exportPath, exportedCsvData)
            exportPath
        } catch (_: IOException) {
            null
        }
    }
}
