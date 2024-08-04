package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.loader.api.FabricLoader
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

            val time = SimpleDateFormat("ss-mm-HH-dd-MM-yyyy").format(Date.from(Instant.now()))
            val path = FabricLoader.getInstance().gameDir.resolve("ledger-export-$time.txt")
            var string = ""
            actions.forEach {
                string += "${it.getMessage(source).string}\n"
            }
            Files.createFile(path)
            Files.writeString(path, string)
            source.sendFeedback({
                Text.translatable(
                "text.ledger.export.completed",
                path.pathString.literal()
                .setStyle(TextColorPallet.primaryVariant)
            ).setStyle(TextColorPallet.primary)
            }, false)
            }
        return 1
    }
}
