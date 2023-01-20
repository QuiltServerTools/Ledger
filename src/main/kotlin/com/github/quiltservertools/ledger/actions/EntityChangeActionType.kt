package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.BlockItem
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.registry.Registries

class EntityChangeActionType : AbstractActionType() {
    override val identifier = "entity-change"

    override fun getTranslationType(): String {
        val item = Registries.ITEM.get(Identifier(extraData))
        return if (item is BlockItem && item !is AliasedBlockItem) {
            "block"
        } else {
            "item"
        }
    }

    override fun getObjectMessage(): Text {
        val text = Text.literal("")
        text.append(
            Text.translatable(
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
            })

        if (extraData != null && Identifier(extraData) != Identifier.tryParse("minecraft:air")) {
            val stack = NbtUtils.itemFromProperties(null, Identifier(extraData))
            text.append(Text.literal(" ").append(Text.translatable("text.ledger.action_message.with")).append(" "))
            text.append(
                Text.translatable(
                    Util.createTranslationKey(
                        this.getTranslationType(),
                        Identifier(extraData)
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

        val oldEntity = StringNbtReader.parse(oldEntityState)
        val uuid = oldEntity!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid)

        if (entity != null) {
            entity.readNbt(oldEntity)
            return true
        }
        return false
    }


    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val newEntity = StringNbtReader.parse(entityState)
        val uuid = newEntity!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid)

        if (entity != null) {
            entity.readNbt(newEntity)
            return true
        }
        return false
    }
}
