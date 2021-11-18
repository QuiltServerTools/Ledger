package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.*
import net.minecraft.item.BlockItem
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
    fun getEntityObjectMessage(): MutableText{

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

    fun getItemObjectMessage(): MutableText{
        val stack = Registry.ITEM.get(oldObjectIdentifier).defaultStack

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

    fun getSpacerObjectMessage(): MutableText{

        return TranslatableText(" $sourceName ").setStyle(TextColorPallet.light).styled {
            it.withHoverEvent(HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                sourceName.toString().literal())
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

        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        return false
    }
}
