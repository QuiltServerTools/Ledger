package com.github.quiltservertools.ledger.commands.parameters

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.IdentifierArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.tag.BlockTags
import net.minecraft.tag.EntityTypeTags
import net.minecraft.tag.ItemTags
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

            val blockTag = BlockTags.getTagGroup().getTag(tagId)
            if (blockTag != null) return blockTag.values().map { Registry.BLOCK.getId(it) }

            val itemTag = ItemTags.getTagGroup().getTag(tagId)
            if (itemTag != null) return itemTag.values().map { Registry.ITEM.getId(it) }

            val entityTag = EntityTypeTags.getTagGroup().getTag(tagId)
            if (entityTag != null) return entityTag.values().map { Registry.ENTITY_TYPE.getId(it) }
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
                    addAll(BlockTags.getTagGroup().tagIds)
                    addAll(ItemTags.getTagGroup().tagIds)
                    addAll(EntityTypeTags.getTagGroup().tagIds)
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
