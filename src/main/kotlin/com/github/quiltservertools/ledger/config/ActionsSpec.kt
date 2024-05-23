package com.github.quiltservertools.ledger.config

import com.github.quiltservertools.ledger.config.util.IdentifierWrapper
import com.uchuhimo.konf.ConfigSpec

object ActionsSpec : ConfigSpec() {
    val typeBlacklist by required<List<String>>()
    val worldBlacklist by required<List<IdentifierWrapper>>()
    val objectBlacklist by required<List<IdentifierWrapper>>()
    val sourceBlacklist by required<List<String>>()
}
