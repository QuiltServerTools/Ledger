package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text

object PageCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("page")
            .then(
                argument("page", IntegerArgumentType.integer(1))
                    .executes { page(it, IntegerArgumentType.getInteger(it, "page")) }
            )
            .build()

    private fun page(context: Context, page: Int): Int {
        val source = context.source

        val params = Ledger.searchCache[source.name]
        if (params != null) {
            Ledger.launch(Dispatchers.IO) {
                MessageUtils.warnBusy(source)
                val results = DatabaseManager.searchActions(params, page)

                if (results.page > results.pages) {
                    source.sendError(Text.translatable("error.ledger.no_more_pages"))
                    return@launch
                }

                MessageUtils.sendSearchResults(
                    source,
                    results,
                    Text.translatable(
                        "text.ledger.header.search"
                    ).setStyle(TextColorPallet.primary)
                )
            }

            return 1
        } else {
            source.sendError(Text.translatable("error.ledger.no_cached_params"))
            return -1
        }
    }
}
