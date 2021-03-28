package us.potatoboy.ledger.commands

import us.potatoboy.ledger.utility.LiteralNode
import net.minecraft.server.command.CommandManager.literal

class AliasNode(private val alias: String, private val node: LiteralNode) : BuildableCommand {
    override fun build(): LiteralNode =
        literal(alias)
            .redirect(node)
            .build()
}