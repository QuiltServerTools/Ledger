package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object DatabaseSpec : ConfigSpec() {
    val queueTimeoutMin by required<Long>()
    val queueCheckDelaySec by required<Long>()
    val autoPurgeDays by required<Int>()
}
