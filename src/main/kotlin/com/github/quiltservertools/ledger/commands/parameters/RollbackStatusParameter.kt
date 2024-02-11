package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

class RollbackStatusParameter : SimpleParameter<Boolean>() {
    override fun parse(stringReader: StringReader): Boolean = BoolArgumentType.bool().parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = CommandSource.suggestMatching(setOf("true", "false"), builder)
}
