package us.potatoboy.ledger.commands

import us.potatoboy.ledger.utility.LiteralNode

interface BuildableCommand {
    fun build(): LiteralNode
}