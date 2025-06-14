package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.LOGGER
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.AbstractDecorationEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.storage.NbtReadView
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.ErrorReporter
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.Uuids

class EntityChangeActionType : AbstractActionType() {
    override val identifier = "entity-change"

    override fun getTranslationType(): String {
        val item = getStack(DynamicRegistryManager.EMPTY).item
        return if (item is BlockItem) {
            "block"
        } else {
            "item"
        }
    }

    private fun getStack(registryManager: DynamicRegistryManager): ItemStack {
        if (extraData == null) return ItemStack.EMPTY
        try {
            val readView = NbtReadView.create(
                ErrorReporter.EMPTY,
                registryManager,
                StringNbtReader.readCompound(extraData)
            )
            return readView.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY)
        } catch (_: CommandSyntaxException) {
            // In an earlier version of ledger extraData only stored the item id
            val item = Registries.ITEM.get(Identifier.of(extraData))
            return item.defaultStack
        }
    }

    override fun getObjectMessage(source: ServerCommandSource): Text {
        val text = Text.literal("")
        text.append(
            Text.translatable(
                Util.createTranslationKey(
                    "entity",
                    objectIdentifier
                )
            ).setStyle(TextColorPallet.secondaryVariant).styled {
                it.withHoverEvent(
                    HoverEvent.ShowText(
                        objectIdentifier.toString().literal()
                    )
                )
            }
        )

        val stack = getStack(source.registryManager)
        if (!stack.isEmpty) {
            text.append(Text.literal(" ").append(Text.translatable("text.ledger.action_message.with")).append(" "))
            text.append(
                Text.translatable(
                    stack.item.translationKey
                ).setStyle(TextColorPallet.secondaryVariant).styled {
                    it.withHoverEvent(
                        HoverEvent.ShowItem(
                            stack
                        )
                    )
                }
            )
        }
        return text
    }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val oldEntity = StringNbtReader.readCompound(oldObjectState)
        val optionalUUID = oldEntity.get(UUID, Uuids.INT_STREAM_CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity != null) {
            ErrorReporter.Logging({ "ledger:rollback:entity-change@$pos" }, LOGGER).use {
                val readView = NbtReadView.create(it, server.registryManager, oldEntity)
                if (entity is ItemFrameEntity) {
                    entity.heldItemStack = ItemStack.EMPTY
                }
                when (entity) {
                    is LivingEntity -> entity.readData(readView)
                    is AbstractDecorationEntity -> entity.readData(readView)
                }
            }
            return true
        }
        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val newEntity = StringNbtReader.readCompound(objectState)
        val optionalUUID = newEntity.get(UUID, Uuids.INT_STREAM_CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity != null) {
            ErrorReporter.Logging({ "ledger:restore:entity-change@$pos" }, LOGGER).use {
                val readView = NbtReadView.create(it, server.registryManager, newEntity)
                if (entity is ItemFrameEntity) {
                    entity.heldItemStack = ItemStack.EMPTY
                }
                when (entity) {
                    is LivingEntity -> entity.readData(readView)
                    is AbstractDecorationEntity -> entity.readData(readView)
                }
            }
            return true
        }
        return false
    }
}
