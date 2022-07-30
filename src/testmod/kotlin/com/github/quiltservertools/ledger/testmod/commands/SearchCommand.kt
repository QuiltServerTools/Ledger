package com.github.quiltservertools.ledger.testmod.commands

import com.github.quiltservertools.ledger.testmod.LedgerTest
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object SearchCommand {
    fun build(): LiteralCommandNode<FabricClientCommandSource> =
        literal("search")
            .then(
                argument("param", StringArgumentType.greedyString())
                    .executes { LedgerTest.sendSearchQuery(StringArgumentType.getString(it, "param")); 1 }
            )
            .build()
}
