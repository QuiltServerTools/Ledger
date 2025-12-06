package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.Util
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

class BlockBreakActionType : BlockChangeActionType() {
    override val identifier = "block-break"

    override fun getObjectMessage(source: CommandSourceStack): Component = Component.translatable(
        Util.makeDescriptionId(
            this.getTranslationType(),
            oldObjectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).withStyle {
        it.withHoverEvent(
            HoverEvent.ShowText(
                oldObjectIdentifier.toString().literal()
            )
        )
    }
}
