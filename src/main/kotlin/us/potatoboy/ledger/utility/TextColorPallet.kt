package us.potatoboy.ledger.utility

import net.minecraft.text.Style
import net.minecraft.text.TextColor
import us.potatoboy.ledger.config.ColorSpec
import us.potatoboy.ledger.config.config

@Suppress("MagicNumber")
object TextColorPallet {
    val primary: Style get() =  Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.primary]))
    val primaryVariant: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.primaryVariant]))
    val secondary: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.secondary]))
    val secondaryVariant: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.secondaryVariant]))
    val light: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.light]))
}
