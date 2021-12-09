package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
import com.github.quiltservertools.ledger.utility.Sources.EQUIP
import com.github.quiltservertools.ledger.utility.Sources.REMOVE
import com.github.quiltservertools.ledger.utility.Sources.ROTATE
import com.github.quiltservertools.ledger.utility.Sources.SHEAR
import net.minecraft.entity.LivingEntity.getPreferredEquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.item.*
import net.minecraft.server.MinecraftServer
import net.minecraft.text.*
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry
import kotlin.experimental.and

class EntityChangeActionType : AbstractActionType() {
    override val identifier = "entity-change"

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(oldObjectIdentifier)
        return if (item is BlockItem && item !is AliasedBlockItem) {
            "block"
        } else if (true){
            "item"
        }else {"entity"}
    }

    override fun getObjectMessage(): Text {
        val text = LiteralText("")
        text.append(
            TranslatableText(
                Util.createTranslationKey(
                    "entity",
                    oldObjectIdentifier
                )
            ).setStyle(TextColorPallet.secondaryVariant).styled {
                it.withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        oldObjectIdentifier.toString().literal()
                    )
                )
            })

        if (objectIdentifier != Identifier.tryParse("minecraft:air")) {
            val stack = NbtUtils.itemFromProperties(extraData, objectIdentifier)
            text.append(" with ".literal())
            text.append(
                TranslatableText(
                    Util.createTranslationKey(
                        this.getTranslationType(),
                        objectIdentifier
                    )
                ).setStyle(TextColorPallet.secondaryVariant).styled {
                    it.withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_ITEM,
                            HoverEvent.ItemStackContent(stack)
                        )
                    )
                })
        }
        return text
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
            }
        } else if (entity is ItemFrameEntity) {
            when (sourceName) {
                REMOVE -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                EQUIP -> { entity.heldItemStack = ItemStack(Items.AIR); return true }
                ROTATE -> { entity.rotation = entity.rotation - 1 ; return true }
                // can only ever rotate by 1
            }
        } else if (entity is SheepEntity) {
            when (sourceName) {
                SHEAR -> entity.isSheared = false
            }
            entity.color = DyeColor.byId((rollbackEntity.getByte(COLOR) and 0xF).toInt())
            return true
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
            }
        } else if (entity is ItemFrameEntity) {
            when (sourceName) {
                EQUIP -> { if (entity.heldItemStack.isEmpty) entity.heldItemStack = rollbackStack; return true }
                REMOVE -> { entity.heldItemStack = ItemStack(Items.AIR); return true }
                ROTATE -> { entity.rotation = entity.rotation + 1 ; return true }
            }
        } else if (entity is SheepEntity) {
            when (sourceName) {
                SHEAR -> entity.isSheared = true
            }
            entity.color = DyeColor.byId((rollbackEntity.getByte(COLOR) and 0xF).toInt())
            return true
        }
        return false
    }
}

