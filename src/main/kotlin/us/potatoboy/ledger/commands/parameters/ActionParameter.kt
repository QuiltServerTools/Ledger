package us.potatoboy.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import us.potatoboy.ledger.registry.ActionRegistry
import java.util.concurrent.CompletableFuture

class ActionParameter : SimpleParameter<String>() {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>?,
        builder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions> {
        return CommandSource.suggestMatching(
            ActionRegistry.getTypes(),
            builder
        )
    }

    override fun parse(stringReader: StringReader): String = StringArgumentType.word().parse(stringReader)
}
