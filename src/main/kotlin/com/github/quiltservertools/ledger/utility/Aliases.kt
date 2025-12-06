package com.github.quiltservertools.ledger.utility

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.commands.CommandSourceStack

typealias Dispatcher = CommandDispatcher<CommandSourceStack>
typealias LiteralNode = LiteralCommandNode<CommandSourceStack>
typealias Context = CommandContext<CommandSourceStack>
