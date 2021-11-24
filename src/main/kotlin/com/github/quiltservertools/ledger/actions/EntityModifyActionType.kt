package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
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
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class EntityModifyActionType:  AbstractActionType()  {
    override val identifier = "entity-modify"

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(oldObjectIdentifier)
        return if (item is BlockItem) {
            "block"
        } else {
            "item"
        }
    }

    private fun getEntityObjectMessage(): MutableText{
        return TranslatableText(
            Util.createTranslationKey(
                "entity",
                objectIdentifier)
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
            return "@$sourceName".literal().setStyle(TextColorPallet.secondary)
        }
        return sourceProfile!!.name.literal().setStyle(TextColorPallet.secondary)
    }

    private fun getItemObjectMessage(): MutableText{
        val stack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        return TranslatableText(
            Util.createTranslationKey(
                getTranslationType(),
                oldObjectIdentifier)
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_ITEM,
                    HoverEvent.ItemStackContent(stack)
                )
            )
        }
    }

    private fun getSpacerObjectMessage(): MutableText{

        return TranslatableText(" @$sourceName ").setStyle(TextColorPallet.light).styled {
            it.withHoverEvent(HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                sourceName.literal())
            )
        }
    }

    override fun getObjectMessage(): Text {
        return getEntityObjectMessage().append(
            getSpacerObjectMessage()).append(
            getItemObjectMessage())
    }


    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val uuid = NbtUtils.entityFromProperties(extraData)!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        if (entity is ArmorStandEntity) {
            // what items does this break with?
            val slot = getPreferredEquipmentSlot(rollbackStack)

            when (sourceName) {
                "Remove" -> if (entity.getEquippedStack(slot).isEmpty) {
                    entity.equipStack(slot, rollbackStack); return true }
                "Equip" -> {entity.equipStack(slot, ItemStack(Items.AIR)); return true }
            }
        }else if (entity is ItemFrameEntity){

            when(sourceName) {
                "Remove" -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                "Equip"  -> { entity.heldItemStack = ItemStack(Items.AIR); return true}
            }
        }
        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val uuid = NbtUtils.entityFromProperties(extraData)!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, oldObjectIdentifier)

        if (entity is ArmorStandEntity) {
            // what items does this break with?
            val slot = getPreferredEquipmentSlot(rollbackStack)

            when (sourceName) {
                "Equip" -> if (entity.getEquippedStack(slot).isEmpty) {
                    entity.equipStack(slot, rollbackStack); return true }
                "Remove" -> {entity.equipStack(slot, ItemStack(Items.AIR)); return true }
            }
        }else if (entity is ItemFrameEntity){

            when(sourceName) {
                "Equip" -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                "Remove"  -> { entity.heldItemStack = ItemStack(Items.AIR); return true}
            }
        }
        return false
    }
}

