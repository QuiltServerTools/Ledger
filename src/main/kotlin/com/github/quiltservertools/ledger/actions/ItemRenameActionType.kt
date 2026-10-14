package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate

/**
 * Only records the rename: the item may have moved anywhere since, so there is nothing to roll back
 * or restore. extraData holds the renamed stack, oldObjectState the stack before the rename.
 */
class ItemRenameActionType : AbstractActionType() {
    override val identifier = "item-rename"

    // Not used
    override fun getTranslationType(): String = "item"

    private fun getStack(server: MinecraftServer, tag: String?): ItemStack = NbtUtils.itemFromProperties(
        tag,
        objectIdentifier,
        server.registryAccess(),
    )

    override fun getObjectMessage(source: CommandSourceStack): Component {
        val newStack = getStack(source.server, extraData)
        val oldStack = getStack(source.server, oldObjectState)

        return "${newStack.count} ".literal().append(
            newStack.itemName,
        ).setStyle(TextColorPallet.secondaryVariant).withStyle {
            it.withHoverEvent(
                HoverEvent.ShowItem(
                    ItemStackTemplate(newStack.item, newStack.componentsPatch),
                ),
            )
        }.append(
            " \"".literal().append(oldStack.hoverName).append("\" → \"").append(newStack.hoverName).append("\"")
                .setStyle(TextColorPallet.secondary),
        )
    }

    override fun getRollbackMessage(source: CommandSourceStack): Component? = null
}
