package us.potatoboy.ledger.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.server.command.ServerCommandSource
import us.potatoboy.ledger.utility.LiteralNode

interface BuildableCommand {
    fun build(): LiteralNode
}