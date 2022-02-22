package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
import net.minecraft.item.*
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.*
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class EntityChangeActionType : AbstractActionType() {
    override val identifier = "entity-change"

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(Identifier(extraData))
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

        if (Identifier(extraData!!) != Identifier.tryParse("minecraft:air")) {
            val stack = NbtUtils.itemFromProperties(null, Identifier(extraData))
            text.append(" with ".literal())
            text.append(
                TranslatableText(
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

        val oldEntity = StringNbtReader.parse(oldEntityState);
        val uuid = oldEntity!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid)

        if (entity != null) {
            entity.readNbt(oldEntity)
            return true
        }
        // dont spawn entity. it should have died after
        return false
    }



    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        //val rollbackExtraData = StringNbtReader.parse(extraData) ?: return false

        //val rollbackStack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

        val newEntity = StringNbtReader.parse(entityState);
        val uuid = newEntity!!.getUuid(UUID) ?: return false
        val entity = world?.getEntity(uuid)

        if (entity != null) {
            entity.readNbt(newEntity)
            return true
        }
        // dont spawn entity. it should have died after
        return false
    }
}
