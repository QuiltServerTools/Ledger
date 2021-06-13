package com.github.quiltservertools.ledger.commands

import com.github.quiltservertools.ledger.utility.LiteralNode

interface BuildableCommand {
    fun build(): LiteralNode
}
