package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.server.command.ServerCommandSource
import java.util.concurrent.CompletableFuture

class RollbackStatusParameter : SimpleParameter<Boolean>() {
    override fun parse(stringReader: StringReader): Boolean = try {
        BoolArgumentType.bool().parse(stringReader)
    } catch (e: CommandSyntaxException) {
        stringReader.readString()
        false // TODO Maybe rework parser to alert errors and not require a default value
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> = BoolArgumentType.bool().listSuggestions(context, builder)
}
