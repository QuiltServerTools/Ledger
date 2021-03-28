package us.potatoboy.ledger.commands

import net.minecraft.server.command.CommandManager.literal
import us.potatoboy.ledger.commands.subcommands.InspectCommand
import us.potatoboy.ledger.commands.subcommands.SearchCommand
import us.potatoboy.ledger.utility.BrigadierUtils
import us.potatoboy.ledger.utility.Dispatcher

class LedgerCommand(val dispatcher: Dispatcher) {
    fun register() {
        val rootNode = literal("ledger").build()

        val inspectNode = InspectCommand().build()

        dispatcher.root.addChild(rootNode)
        dispatcher.root.addChild(AliasNode("lg", rootNode).build())

        rootNode.addChild(inspectNode)
        rootNode.addChild(BrigadierUtils.buildRedirect("i", inspectNode))

        rootNode.addChild(SearchCommand().build())
    }
}