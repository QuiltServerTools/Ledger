package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.config.ColorSpec
import com.github.quiltservertools.ledger.config.config
import com.mojang.serialization.DataResult
import net.minecraft.text.Style
import net.minecraft.text.TextColor

@Suppress("MagicNumber")
object TextColorPallet {
    val primary: Style
        get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.primary]).getOrNull())

    val primaryVariant: Style get() = Style.EMPTY.withColor(
        TextColor.parse(config[ColorSpec.primaryVariant]).getOrNull()
    )
    val secondary: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.secondary]).getOrNull())
    val secondaryVariant: Style get() = Style.EMPTY.withColor(
        TextColor.parse(config[ColorSpec.secondaryVariant]).getOrNull()
    )
    val light: Style get() = Style.EMPTY.withColor(TextColor.parse(config[ColorSpec.light]).getOrNull())
}

fun DataResult<TextColor>.getOrNull(): TextColor? = this.result().orElse(null)
