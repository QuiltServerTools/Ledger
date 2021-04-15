package us.potatoboy.ledger.actions

import net.minecraft.text.HoverEvent
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.utility.literal

class BlockBreakActionType : BlockChangeActionType("block-break") {
    override fun getObjectMessage(): Text = TranslatableText(
        Util.createTranslationKey(
            this.getTranslationType(),
            oldObjectIdentifier
        )
    ).setStyle(TextColorPallet.tertiary).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                oldObjectIdentifier.toString().literal()
            )
        )
    }
}