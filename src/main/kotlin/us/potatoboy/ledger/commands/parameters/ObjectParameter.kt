package us.potatoboy.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.concurrent.CompletableFuture

class ObjectParameter : SimpleParameter<Identifier>() {
    private val identifiers = mutableListOf<Identifier>()

    init {
        identifiers.addAll(Registry.ITEM.ids)
        identifiers.addAll(Registry.BLOCK.ids)
        identifiers.addAll(Registry.ENTITY_TYPE.ids)
    }

    override fun parse(stringReader: StringReader): Identifier = IdentifierArgumentType.identifier().parse(stringReader)

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>?,
        builder: SuggestionsBuilder?
    ): CompletableFuture<Suggestions> {
        return CommandSource.suggestIdentifiers(
            identifiers,
            builder
        )
    }
}
