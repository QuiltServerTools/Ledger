package com.github.quiltservertools.ledger.commands

import com.github.quiltservertools.ledger.api.ExtensionManager
import com.github.quiltservertools.ledger.commands.subcommands.InspectCommand
import com.github.quiltservertools.ledger.commands.subcommands.PageCommand
import com.github.quiltservertools.ledger.commands.subcommands.PlayerCommand
import com.github.quiltservertools.ledger.commands.subcommands.PreviewCommand
import com.github.quiltservertools.ledger.commands.subcommands.PurgeCommand
import com.github.quiltservertools.ledger.commands.subcommands.RestoreCommand
import com.github.quiltservertools.ledger.commands.subcommands.RollbackCommand
import com.github.quiltservertools.ledger.commands.subcommands.SearchCommand
import com.github.quiltservertools.ledger.commands.subcommands.StatusCommand
import com.github.quiltservertools.ledger.commands.subcommands.TeleportCommand
import com.github.quiltservertools.ledger.utility.BrigadierUtils
import com.github.quiltservertools.ledger.utility.Dispatcher
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.server.command.CommandManager.literal

fun registerCommands(dispatcher: Dispatcher) {
    val rootNode =
        literal("ledger").requires(Permissions.require("ledger.commands.root", CommandConsts.PERMISSION_LEVEL))
            .build()

    dispatcher.root.addChild(rootNode)
    dispatcher.root.addChild(
        literal("lg")
            .requires(Permissions.require("ledger.commands.root", CommandConsts.PERMISSION_LEVEL)).redirect(rootNode)
            .build()
    )

    rootNode.addChild(InspectCommand.build())
    rootNode.addChild(BrigadierUtils.buildRedirect("i", InspectCommand.build()))

    rootNode.addChild(SearchCommand.build())
    rootNode.addChild(BrigadierUtils.buildRedirect("s", SearchCommand.build()))

    rootNode.addChild(PageCommand.build())
    rootNode.addChild(BrigadierUtils.buildRedirect("pg", PageCommand.build()))

    rootNode.addChild(RollbackCommand.build())
    rootNode.addChild(BrigadierUtils.buildRedirect("rb", RollbackCommand.build()))

    rootNode.addChild(PreviewCommand.build())
    rootNode.addChild(BrigadierUtils.buildRedirect("pv", PreviewCommand.build()))

    rootNode.addChild(RestoreCommand.build())

    rootNode.addChild(StatusCommand.build())

    rootNode.addChild(TeleportCommand.build())

    rootNode.addChild(PurgeCommand.build())

    rootNode.addChild(PlayerCommand.build())

    ExtensionManager.commands.forEach {
        it.registerSubcommands().forEach { command ->
            rootNode.addChild(command.build())
        }
    }
}
