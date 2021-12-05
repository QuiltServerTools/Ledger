package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
import com.github.quiltservertools.ledger.utility.Sources.EQUIP
import com.github.quiltservertools.ledger.utility.Sources.REMOVE
import com.github.quiltservertools.ledger.utility.Sources.ROTATE
import net.minecraft.entity.LivingEntity.getPreferredEquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class EntityModifyActionType : AbstractActionType() {
    override val identifier = "entity-modify"

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(oldObjectIdentifier)
        return if (item is BlockItem) {
            "block"
        } else {
            "item"
        }
    }

    private fun getEntityObjectMessage(): MutableText {
        return TranslatableText(
            Util.createTranslationKey(
                "entity",
                objectIdentifier
            )
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    objectIdentifier.toString().literal()
                )
            )
        }
    }

    override fun getSourceMessage(): Text {
        if (sourceProfile == null) {
            return TranslatableText("@").append(TranslatableText(
                Util.createTranslationKey(
                    "entity",
                    objectIdentifier
                ))).setStyle(TextColorPallet.secondary)
        }
        return sourceProfile!!.name.literal().setStyle(TextColorPallet.secondary)
    }

    private fun getItemObjectMessage(): MutableText {
        val stack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        return TranslatableText(
            Util.createTranslationKey(
                getTranslationType(),
                oldObjectIdentifier
            )
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_ITEM,
                    HoverEvent.ItemStackContent(stack)
                )
            )
        }
    }

    private fun getSourceObjectMessage(): MutableText {

        return TranslatableText(" @$sourceName ").setStyle(TextColorPallet.light).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    sourceName.literal()
                )
            )
        }
    }

    override fun getObjectMessage(): Text {
        return getEntityObjectMessage().append(
            if (oldObjectIdentifier != Identifier("minecraft:air")) {
                getSourceObjectMessage().append(getItemObjectMessage())
            } else {getSourceObjectMessage()})
    }


    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val rollbackEntity = NbtUtils.entityFromProperties(extraData) ?: return false
        val entity = world?.getEntity(rollbackEntity.getUuid(UUID) ) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        if (entity is ArmorStandEntity) {
            val slot = getPreferredEquipmentSlot(rollbackStack)
            when (sourceName) {
                REMOVE-> { if (entity.getEquippedStack(slot).isEmpty) entity.equipStack(slot, rollbackStack); return true }
                EQUIP -> { entity.equipStack(slot, ItemStack(Items.AIR)); return true }
                ROTATE -> { entity.readCustomDataFromNbt(rollbackEntity) ; return true}
            }
        } else if (entity is ItemFrameEntity) {
            when (sourceName) {
                REMOVE -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                EQUIP -> { entity.heldItemStack = ItemStack(Items.AIR); return true }
                ROTATE -> { entity.rotation = entity.rotation - 1 ; return true }
                // can only ever rotate by 1
            }
        }
        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val rollbackEntity = NbtUtils.entityFromProperties(extraData) ?: return false
        val entity = world?.getEntity(rollbackEntity.getUuid(UUID)) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        if (entity is ArmorStandEntity) {
            val slot = getPreferredEquipmentSlot(rollbackStack)
            when (sourceName) {
                EQUIP -> { if (entity.getEquippedStack(slot).isEmpty) entity.equipStack(slot, rollbackStack); return true }
                REMOVE-> { entity.equipStack(slot, ItemStack(Items.AIR)); return true }
                ROTATE -> { entity.readCustomDataFromNbt(rollbackEntity) ; return true}
            }
        } else if (entity is ItemFrameEntity) {
            when (sourceName) {
                EQUIP -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                REMOVE -> { entity.heldItemStack = ItemStack(Items.AIR); return true }
                ROTATE -> { entity.rotation = entity.rotation + 1 ; return true }
            }
        }
        return false
    }
}

