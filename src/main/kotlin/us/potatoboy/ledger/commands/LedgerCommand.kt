package us.potatoboy.ledger.commands

import net.minecraft.server.command.CommandManager.literal
import us.potatoboy.ledger.commands.subcommands.*
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

        rootNode.addChild(StatusCommand.build())
    }
}