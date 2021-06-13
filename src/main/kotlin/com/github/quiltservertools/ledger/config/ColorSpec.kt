package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object ColorSpec : ConfigSpec() {
    val primary by required<String>()
    val primaryVariant by required<String>()
    val secondary by required<String>()
    val secondaryVariant by required<String>()
    val light by required<String>()
}
