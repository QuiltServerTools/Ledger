package com.github.quiltservertools.ledger.utility

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.server.command.ServerCommandSource

typealias Dispatcher = CommandDispatcher<ServerCommandSource>
typealias LiteralNode = LiteralCommandNode<ServerCommandSource>
typealias Context = CommandContext<ServerCommandSource>
