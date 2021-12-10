package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
import com.github.quiltservertools.ledger.utility.Sources.DYE
import com.github.quiltservertools.ledger.utility.Sources.EQUIP
import com.github.quiltservertools.ledger.utility.Sources.REMOVE
import com.github.quiltservertools.ledger.utility.Sources.ROTATE
import com.github.quiltservertools.ledger.utility.Sources.SHEAR
import net.minecraft.entity.LivingEntity.getPreferredEquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.passive.CatEntity
import net.minecraft.entity.passive.SheepEntity
import net.minecraft.entity.passive.WolfEntity
import net.minecraft.item.*
import net.minecraft.server.MinecraftServer
import net.minecraft.text.*
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class EntityChangeActionType : AbstractActionType() {
    override val identifier = "entity-change"

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(oldObjectIdentifier)
        return if (item is BlockItem && item !is AliasedBlockItem) {
            "block"
        } else {
            "item"
        }
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
        val entity = world?.getEntity(rollbackEntity.getUuid(UUID)) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

        when (entity) {
            is ArmorStandEntity -> when (sourceName) {
                REMOVE -> {
                    val slot = getPreferredEquipmentSlot(rollbackStack)
                    if (entity.getEquippedStack(slot).isEmpty) {
                        entity.equipStack(slot, rollbackStack)
                        return true
                    }
                }
                EQUIP -> {
                    val slot = getPreferredEquipmentSlot(rollbackStack)
                    entity.equipStack(slot, ItemStack(Items.AIR))
                    return true
                }
            }
            is ItemFrameEntity -> when (sourceName) {
                REMOVE -> {
                    if (entity.heldItemStack.isEmpty) {
                        entity.heldItemStack = rollbackStack;
                        return true
                    }
                }
                EQUIP -> {
                    entity.heldItemStack = ItemStack(Items.AIR)
                    return true
                }
                ROTATE -> {
                    entity.rotation = entity.rotation - 1
                    return true
                } // can only ever rotate by 1
            }

            is SheepEntity -> when (sourceName) {
                SHEAR -> {
                    entity.isSheared = false
                    return true
                }
                DYE -> {
                    entity.color = DyeColor.byId(rollbackEntity.getByte(COLOR).toInt())
                    return true
                }
            }
            is WolfEntity -> when (sourceName) {
                DYE -> {
                    entity.collarColor = DyeColor.byId(rollbackEntity.getByte(COLLARCOLOR).toInt())
                    return true
                }
            }
            is CatEntity -> when (sourceName) {
                    DYE -> {
                        entity.collarColor = DyeColor.byId(rollbackEntity.getByte(COLLARCOLOR).toInt())
                        return true
                    }
                }
        }
        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val rollbackEntity = NbtUtils.entityFromProperties(extraData) ?: return false
        val entity = world?.getEntity(rollbackEntity.getUuid(UUID)) ?: return false

        val rollbackStack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

        when (entity) {
            is ArmorStandEntity -> when (sourceName) {
                    EQUIP -> {
                        val slot = getPreferredEquipmentSlot(rollbackStack)
                        if (entity.getEquippedStack(slot).isEmpty) {
                            entity.equipStack(slot, rollbackStack)
                            return true
                        }
                    }
                    REMOVE -> {
                        val slot = getPreferredEquipmentSlot(rollbackStack)
                        entity.equipStack(slot, ItemStack(Items.AIR))
                        return true
                    }
                }
            is ItemFrameEntity -> when (sourceName) {
                    EQUIP -> {
                        if (entity.heldItemStack.isEmpty) {
                            entity.heldItemStack = rollbackStack;
                            return true
                        }
                    }
                    REMOVE -> {
                        entity.heldItemStack = ItemStack(Items.AIR)
                        return true
                    }
                    ROTATE -> {
                        entity.rotation = entity.rotation + 1
                        return true
                    } // can only ever rotate by 1
                }

            is SheepEntity -> when (sourceName) {
                    SHEAR -> {
                        entity.isSheared = true
                        return true
                    }
                    DYE -> {
                        entity.color = (rollbackStack.item as DyeItem).color
                        return true
                    }
                }
            is WolfEntity -> when (sourceName) {
                    DYE -> {
                        entity.collarColor = (rollbackStack.item as DyeItem).color
                        return true
                    }
                }
            is CatEntity -> when (sourceName) {
                    DYE -> {
                        entity.collarColor = (rollbackStack.item as DyeItem).color
                        return true
                    }
                }

        }
        return false
    }
}
