package com.github.quiltservertools.ledger.commands.parameters

import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.Negatable
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

class ActionParameter : SimpleParameter<String>() {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>?,
        builder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions> {
        val types = ActionRegistry.getTypes()
        // Need to check equality to catch null
        return if (builder?.remaining?.startsWith('!') == true) {
            CommandSource.suggestMatching(
                types.map { "!$it" },
                builder
            )
        } else {
            CommandSource.suggestMatching(
                types,
                builder
            )
        }
    }

    override fun parse(stringReader: StringReader): Negatable<String> = Negatable.getNegatable(stringReader, StringArgumentType.word())
}
