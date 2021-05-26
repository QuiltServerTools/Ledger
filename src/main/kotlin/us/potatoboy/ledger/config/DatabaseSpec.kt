package us.potatoboy.ledger.config

import com.uchuhimo.konf.ConfigSpec

object DatabaseSpec : ConfigSpec() {
    val maxQueueSize by required<Int>()
    val queueTimeoutSec by required<Long>()
}
