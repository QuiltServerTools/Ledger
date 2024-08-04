package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.config.getExportDir
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.io.path.pathString
import kotlin.time.ExperimentalTime

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

    @OptIn(ExperimentalTime::class)
    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private fun run(source: ServerCommandSource, params: ActionSearchParams): Int {
        Ledger.launch {
            // query from db
            source.sendFeedback(
                { Text.translatable("text.ledger.export.started").setStyle(TextColorPallet.primary) },
                false
            )
            var results = DatabaseManager.searchActions(params, 0)
            val actions = mutableListOf<ActionType>()
            for (i in results.page..results.pages) {
                results = DatabaseManager.searchActions(params, i)
                results.actions.forEach {
                    actions.add(it)
                }
            }
            source.sendFeedback({
                Text.translatable(
                    "text.ledger.export.actions",
                    results.pages.toString().literal()
                    .setStyle(TextColorPallet.secondaryVariant)
                ).setStyle(TextColorPallet.secondary)
            }, false)

            // export to file
            val time = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(Date.from(Instant.now()))
            var string = ""
            actions.forEach {
                string += "${it.getMessage(source).string}\n"
            }
            val exportDir = config.getExportDir()
            exportDir.toFile().mkdirs()
            val exportPath = exportDir.resolve("ledger-export-$time.txt")
            Files.createFile(exportPath)
            Files.writeString(exportPath, string)
            source.sendFeedback({
                Text.translatable(
                    "text.ledger.export.completed",
                    exportPath.pathString.literal()
                    .setStyle(TextColorPallet.primaryVariant)
                ).setStyle(TextColorPallet.primary)
            }, false)
        }
        return 1
    }
}
