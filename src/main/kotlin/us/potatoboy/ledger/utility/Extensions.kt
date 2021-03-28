package us.potatoboy.ledger.utility

import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.text.Text

fun MutableText.appendWithSpace(text: Text) {
    this.append(text)
    this.append(LiteralText(" "))
}