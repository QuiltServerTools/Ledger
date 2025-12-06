package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.mojang.brigadier.arguments.IntegerArgumentType
import kotlinx.coroutines.launch
import net.minecraft.commands.Commands.argument
import net.minecraft.commands.Commands.literal
import net.minecraft.network.chat.Component

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

        val params = Ledger.searchCache[source.textName]
        if (params != null) {
            Ledger.launch {
                MessageUtils.warnBusy(source)
                val results = DatabaseManager.searchActions(params, page)

                if (results.page > results.pages) {
                    source.sendFailure(Component.translatable("error.ledger.no_more_pages"))
                    return@launch
                }

                MessageUtils.sendSearchResults(
                    source,
                    results,
                    Component.translatable(
                        "text.ledger.header.search"
                    ).setStyle(TextColorPallet.primary)
                )
            }

            return 1
        } else {
            source.sendFailure(Component.translatable("error.ledger.no_cached_params"))
            return -1
        }
    }
}
