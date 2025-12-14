package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import java.util.concurrent.CompletableFuture

class ObjectParameter : SimpleParameter<List<Identifier>>() {
    private val identifiers = mutableListOf<Identifier>().apply {
        addAll(BuiltInRegistries.ITEM.keySet())
        addAll(BuiltInRegistries.BLOCK.keySet())
        addAll(BuiltInRegistries.ENTITY_TYPE.keySet())
    }

    override fun parse(stringReader: StringReader): List<Identifier> {
        if (stringReader.string.isEmpty()) return listOf()
        if (stringReader.string[stringReader.cursor] == '#') {
            stringReader.skip()
            val tagId = IdentifierArgument.id().parse(stringReader)

            val blockTag = TagKey.create(Registries.BLOCK, tagId)
            if (blockTag != null) {
                return BuiltInRegistries.BLOCK.getTagOrEmpty(
                    blockTag
                ).map { BuiltInRegistries.BLOCK.getKey(it.value()) }
            }

            val itemTag = TagKey.create(Registries.ITEM, tagId)
            if (itemTag != null) {
                BuiltInRegistries.ITEM.getTagOrEmpty(
                    itemTag
                ).map { BuiltInRegistries.ITEM.getKey(it.value()) }
            }

            val entityTag = TagKey.create(Registries.ENTITY_TYPE, tagId)
            if (entityTag != null) {
                return BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(entityTag).map {
                BuiltInRegistries.ENTITY_TYPE.getKey(it.value())
            }
            }
        }

        return listOf(IdentifierArgument.id().parse(stringReader))
    }

    override fun getSuggestions(
        context: CommandContext<CommandSourceStack>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        return if (builder.remaining.startsWith("#")) {
            SharedSuggestionProvider.suggestResource(
                mutableListOf<Identifier>().apply {
                    addAll(BuiltInRegistries.BLOCK.tags.map { it.key().location }.toList())
                    addAll(BuiltInRegistries.ITEM.tags.map { it.key().location }.toList())
                    addAll(BuiltInRegistries.ENTITY_TYPE.tags.map { it.key().location }.toList())
                },
                builder.createOffset(builder.start + 1)
            )
        } else {
            SharedSuggestionProvider.suggestResource(
                identifiers,
                builder
            )
        }
    }
}
