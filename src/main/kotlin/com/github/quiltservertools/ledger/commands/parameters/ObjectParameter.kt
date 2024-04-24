package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Identifier
import java.util.concurrent.CompletableFuture

class ObjectParameter : SimpleParameter<List<Identifier>>() {
    private val identifiers = mutableListOf<Identifier>().apply {
        addAll(Registries.ITEM.ids)
        addAll(Registries.BLOCK.ids)
        addAll(Registries.ENTITY_TYPE.ids)
    }

    override fun parse(stringReader: StringReader): List<Identifier> {
        if (stringReader.string.isEmpty()) return listOf()
        if (stringReader.string[stringReader.cursor] == '#') {
            stringReader.skip()
            val tagId = IdentifierArgumentType.identifier().parse(stringReader)

            val blockTag = TagKey.of(RegistryKeys.BLOCK, tagId)
            if (blockTag != null) {
                return Registries.BLOCK.iterateEntries(
                    blockTag
                ).map { Registries.BLOCK.getId(it.value()) }
            }

            val itemTag = TagKey.of(RegistryKeys.ITEM, tagId)
            if (itemTag != null) Registries.ITEM.iterateEntries(itemTag).map { Registries.ITEM.getId(it.value()) }

            val entityTag = TagKey.of(RegistryKeys.ENTITY_TYPE, tagId)
            if (entityTag != null) {
                return Registries.ENTITY_TYPE.iterateEntries(entityTag).map {
                Registries.ENTITY_TYPE.getId(it.value())
            }
            }
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
                    addAll(Registries.BLOCK.streamTags().map { it.id }.toList())
                    addAll(Registries.ITEM.streamTags().map { it.id }.toList())
                    addAll(Registries.ENTITY_TYPE.streamTags().map { it.id }.toList())
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
