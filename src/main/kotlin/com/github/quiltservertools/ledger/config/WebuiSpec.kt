package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object WebuiSpec : ConfigSpec() {
    val webui by required<Boolean>()
    val port by required<Int>()
}
