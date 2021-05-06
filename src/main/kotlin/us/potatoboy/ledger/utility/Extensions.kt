package us.potatoboy.ledger.utility

import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText

fun MutableText.appendWithSpace(text: Text) {
    this.append(text)
    this.append(" ".literal())
}

fun String.literal() = LiteralText(this)
fun String.translate() = TranslatableText(this)
//fun String.translate(vararg args: Any) = TranslatableText(this, args)
