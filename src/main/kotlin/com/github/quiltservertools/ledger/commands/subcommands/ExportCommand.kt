package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.Commands
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component
import org.jetbrains.exposed.v1.jdbc.Database

object ExportCommand : BuildableCommand {
    override fun build(): LiteralNode = literal("export")
        .requires(Permissions.require("ledger.commands.export", CommandConsts.PERMISSION_LEVEL))
        .then(
            Commands.argument("jdbc_url", StringArgumentType.string()).executes {
                runMigrate(
                    it,
                    StringArgumentType.getString(it, "jdbc_url"),
                )
            }.then(
                Commands.argument("batch_size", IntegerArgumentType.integer(1)).executes {
                    runMigrate(
                        it,
                        StringArgumentType.getString(it, "jdbc_url"),
                        IntegerArgumentType.getInteger(it, "batch_size"),
                    )
                },
            ),
        )
        .build()

    private fun runMigrate(ctx: Context, url: String, batchSize: Int? = null): Int {
        val source = ctx.source
        source.sendSuccess(
            { Component.translatable("text.ledger.export.starting").setStyle(TextColorPallet.secondary) },
            true,
        )
        Ledger.launch {
            val to = Database.connect(url)
            DatabaseManager.exportTo(to, batchSize)
            source.sendSuccess(
                { Component.translatable("text.ledger.export.complete").setStyle(TextColorPallet.secondary) },
                true,
            )
        }
        return 1
    }
}
