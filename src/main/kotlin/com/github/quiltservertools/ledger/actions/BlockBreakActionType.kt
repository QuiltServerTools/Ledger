package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util

class BlockBreakActionType : BlockChangeActionType() {
    override val identifier = "block-break"

    override fun getObjectMessage(): Text = Text.translatable(
        Util.createTranslationKey(
            this.getTranslationType(),
            oldObjectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                oldObjectIdentifier.toString().literal()
            )
        )
    }
}
