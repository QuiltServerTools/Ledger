package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.concurrent.CompletableFuture

class ObjectParameter : SimpleParameter<List<Identifier>>() {
    private val identifiers = mutableListOf<Identifier>().apply {
        addAll(Registry.ITEM.ids)
        addAll(Registry.BLOCK.ids)
        addAll(Registry.ENTITY_TYPE.ids)
    }

    override fun parse(stringReader: StringReader): List<Identifier> {
        if (stringReader.string.isEmpty()) return listOf()
        if (stringReader.string[stringReader.cursor] == '#') {
            stringReader.skip()
            val tagId = IdentifierArgumentType.identifier().parse(stringReader)

            val blockTag = TagKey.of(Registry.BLOCK_KEY, tagId)
            if (blockTag != null) return Registry.BLOCK.iterateEntries(blockTag).map { Registry.BLOCK.getId(it.value()) }

            val itemTag = TagKey.of(Registry.ITEM_KEY, tagId)
            if (itemTag != null) Registry.ITEM.iterateEntries(itemTag).map { Registry.ITEM.getId(it.value()) }

            val entityTag = TagKey.of(Registry.ENTITY_TYPE_KEY, tagId)
            if (entityTag != null) return Registry.ENTITY_TYPE.iterateEntries(entityTag).map { Registry.ENTITY_TYPE.getId(it.value()) }
        }

        return listOf(IdentifierArgumentType.identifier().parse(stringReader))
    }

    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return if (builder.remaining.startsWith("#")) {
            CommandSource.suggestIdentifiers(
                mutableListOf<Identifier>().apply {
                    addAll(Registry.BLOCK.streamTags().map { it.id }.toList())
                    addAll(Registry.ITEM.streamTags().map { it.id }.toList())
                    addAll(Registry.ENTITY_TYPE.streamTags().map { it.id }.toList())
                },
                builder.createOffset(builder.start + 1)
            )
        } else {
            CommandSource.suggestIdentifiers(
                identifiers,
                builder
            )
        }
    }
}
