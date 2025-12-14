package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.minecraft.commands.CommandSourceStack

abstract class SimpleParameter<T> : SuggestionProvider<CommandSourceStack> {
    abstract fun parse(stringReader: StringReader): T
}
