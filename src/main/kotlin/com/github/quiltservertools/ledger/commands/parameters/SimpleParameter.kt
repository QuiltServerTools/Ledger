package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.server.command.ServerCommandSource

abstract class SimpleParameter<T> : SuggestionProvider<ServerCommandSource> {
    abstract fun parse(stringReader: StringReader): T
}
