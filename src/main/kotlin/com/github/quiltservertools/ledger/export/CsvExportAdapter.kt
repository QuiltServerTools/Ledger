package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.AbstractActionType
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.utility.MessageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.text.Text
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Path
import java.time.Instant

class CsvExportAdapter : AbstractExportAdapter() {
    private lateinit var exportFileStream: FileOutputStream
    private lateinit var exportFileWriter: OutputStreamWriter

    private fun actionToCsvData(action: AbstractActionType): String {
        val timeStr: String = MessageUtils.instantTimeToFullText(action.timestamp).string
        val sourceName: String = action.getSourceMessage().string
        val actionName: String = action.getActionMessage().string
        val objectName: String = action.getObjectMessage().string
        val x = action.pos.x
        val y = action.pos.y
        val z = action.pos.z
        val worldName: String = action.world?.toString() ?: ""
        val extraData: String = if (action.extraData == null) {
            ""
        } else {
            // Escape " in data and finally wrap "" to avoid bad column
            val escapedExtraData = action.extraData!!.replace("\"", "\"\"")
            "\"${escapedExtraData}\""
        }
        return "$timeStr,$sourceName,$actionName,$objectName,$x,$y,$z,$worldName,$extraData"
    }

    @Throws(IOException::class)
    override suspend fun startExport(exportDir: Path): Path? {
        val time = MessageUtils.instantTimeToFullText(Instant.now(), "yyyy-MM-dd_HH-mm-ss").string
        val exportPath = exportDir.resolve("ledger-export-$time.csv")
        exportFileStream = FileOutputStream(exportPath.normalize().toFile())
        exportFileWriter = OutputStreamWriter(exportFileStream, "UTF-8")
        exportFileWriter.append("${Text.translatable("text.ledger.export.csvTitle").string}\n")
        return exportPath
    }

    @Throws(IOException::class)
    override suspend fun addData(actions: List<ActionType>): Boolean {
        withContext(Dispatchers.IO) {
            actions.forEach {
                exportFileWriter.append(actionToCsvData(it as AbstractActionType))
                exportFileWriter.append("\n")
            }
        }
        return true
    }

    @Throws(IOException::class)
    override suspend fun endExport(): Boolean {
        exportFileWriter.close()
        exportFileStream.close()
        return true
    }
}
