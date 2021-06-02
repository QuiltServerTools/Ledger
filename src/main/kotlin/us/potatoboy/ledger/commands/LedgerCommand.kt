package us.potatoboy.ledger.commands

import net.minecraft.server.command.CommandManager.literal
import us.potatoboy.ledger.commands.subcommands.InspectCommand
import us.potatoboy.ledger.commands.subcommands.PageCommand
import us.potatoboy.ledger.commands.subcommands.PreviewCommand
import us.potatoboy.ledger.commands.subcommands.RestoreCommand
import us.potatoboy.ledger.commands.subcommands.RollbackCommand
import us.potatoboy.ledger.commands.subcommands.SearchCommand
import us.potatoboy.ledger.commands.subcommands.StatusCommand
import us.potatoboy.ledger.commands.subcommands.TeleportCommand
import us.potatoboy.ledger.utility.BrigadierUtils
import us.potatoboy.ledger.utility.Dispatcher

class LedgerCommand(private val dispatcher: Dispatcher) {
    fun register() {
        val rootNode = literal("ledger").build()

        dispatcher.root.addChild(rootNode)
        dispatcher.root.addChild(literal("lg").redirect(rootNode).build())

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
    }
}
