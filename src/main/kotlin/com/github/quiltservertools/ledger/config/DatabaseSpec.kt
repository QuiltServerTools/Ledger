package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

@Suppress("MagicNumber")
object DatabaseSpec : ConfigSpec() {
    val queueTimeoutMin by required<Long>()
    val queueCheckDelaySec by required<Long>()
    val autoPurgeDays by required<Int>()
    val batchSize by optional<Int>(1000)
    val batchDelay by optional<Int>(10)
}
