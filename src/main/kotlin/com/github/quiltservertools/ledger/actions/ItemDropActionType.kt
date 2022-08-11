package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.BlockItem
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

open class ItemDropActionType : AbstractActionType() {
    override val identifier = "item-drop"

    override fun rollback(server: MinecraftServer): Boolean = true
    override fun restore(server: MinecraftServer): Boolean = true

    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(objectIdentifier)
        return if (item is BlockItem && item !is AliasedBlockItem) {
            "block"
        } else {
            "item"
        }
    }

    override fun getObjectMessage(): Text {
        val stack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

        return "${stack.count} ".literal().append(
            Text.translatable(
                Util.createTranslationKey(
                    getTranslationType(), objectIdentifier
                )
            )
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_ITEM, HoverEvent.ItemStackContent(stack)
                )
            )
        }
    }
}
