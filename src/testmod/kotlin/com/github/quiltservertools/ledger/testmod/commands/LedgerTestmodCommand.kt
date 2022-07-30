package com.github.quiltservertools.ledger.testmod.commands

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
    val rootNode =
        literal("ledger-testmod")
            .build()

    dispatcher.root.addChild(rootNode)
    rootNode.addChild(InspectCommand.build())

    rootNode.addChild(SearchCommand.build())
}
