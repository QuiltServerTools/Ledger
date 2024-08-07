package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.config.ExportSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.config.getExportDir
import com.github.quiltservertools.ledger.export.CsvExportAdapter
import com.github.quiltservertools.ledger.export.DataExporter
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.nio.file.Path
import java.util.*
import kotlin.io.path.pathString

object ExportCommand : BuildableCommand {
    override fun build(): LiteralNode {
        return literal("export")
            .requires(Permissions.require("ledger.commands.export", CommandConsts.PERMISSION_LEVEL))
            .then(
                SearchParamArgument.argument(CommandConsts.PARAMS)
                .executes {
                    run(it.source, SearchParamArgument.get(it, CommandConsts.PARAMS))
                }
            )
            .build()
    }

    @Suppress("UseIfInsteadOfWhen")
    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private fun run(source: ServerCommandSource, params: ActionSearchParams): Int {
        Ledger.launch {
            source.sendFeedback(
                { Text.translatable("text.ledger.export.started").setStyle(TextColorPallet.primary) },
                false
            )

            // currently only support CSV export
            val exportAdapter = when (Ledger.config[ExportSpec.format]) {
                "csv" -> CsvExportAdapter()
                else -> CsvExportAdapter()
            }
            val dataExporter = DataExporter(params, exportAdapter, source)

            val exportedFilePath: Path? = dataExporter.exportTo(config.getExportDir())
            if (exportedFilePath == null) {
                source.sendFeedback({
                    Text.translatable("text.ledger.export.failed").setStyle(TextColorPallet.primary)
                }, false)
            } else {
                source.sendFeedback({
                    Text.translatable(
                        "text.ledger.export.completed",
                        exportedFilePath.pathString.literal()
                            .setStyle(TextColorPallet.primaryVariant)
                    ).setStyle(TextColorPallet.primary)
                }, false)
            }
        }
        return 1
    }
}
