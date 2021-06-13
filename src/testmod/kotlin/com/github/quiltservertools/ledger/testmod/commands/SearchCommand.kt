package com.github.quiltservertools.ledger.testmod.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource
import com.github.quiltservertools.ledger.testmod.LedgerTest

object SearchCommand {
    fun build(): LiteralCommandNode<FabricClientCommandSource> =
        literal("search")
            .then(
                argument("param", StringArgumentType.greedyString())
                    .executes { LedgerTest.sendSearchQuery(StringArgumentType.getString(it, "param")); 1 }
            )
            .build()
}
