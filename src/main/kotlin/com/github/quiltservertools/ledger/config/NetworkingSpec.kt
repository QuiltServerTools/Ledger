package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object NetworkingSpec : ConfigSpec() {
    val networking by required<Boolean>()
}
