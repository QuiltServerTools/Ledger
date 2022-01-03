package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object DatabaseSpec : ConfigSpec() {
    val queueTimeoutMin by required<Long>()
    val queueCheckDelaySec by required<Long>()
    val autoPurgeParams by required<List<String>>()
}
