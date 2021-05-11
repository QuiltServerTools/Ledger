package us.potatoboy.ledger.utility

import net.minecraft.text.Style
import net.minecraft.text.TextColor

@Suppress("MagicNumber")
object TextColorPallet {
    val primary: Style = Style.EMPTY.withColor(TextColor.fromRgb(0xA4243B))
    val secondary: Style = Style.EMPTY.withColor(TextColor.fromRgb(0xD8973C))
    val tertiary: Style = Style.EMPTY.withColor(TextColor.fromRgb(0xBD632F))
    val quaternary: Style = Style.EMPTY.withColor(TextColor.fromRgb(0x273E47))
    val light: Style = Style.EMPTY.withColor(TextColor.fromRgb(0xD8C99B))
}
