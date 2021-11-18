package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import net.minecraft.item.BlockItem
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class EntityModifyActionType:  AbstractActionType()  {
    override val identifier = "entity-modify"
    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(objectIdentifier)
        return if (item is BlockItem) {
            "block"
        } else {
            "item"
        }
    }

    override fun getObjectMessage(): Text {
        val stack = Registry.ITEM.get(objectIdentifier).defaultStack

        return TranslatableText(
                Util.createTranslationKey(
                    getTranslationType(),
                    objectIdentifier
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

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        return false
    }
}
