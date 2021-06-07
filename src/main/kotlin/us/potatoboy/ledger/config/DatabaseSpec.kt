package us.potatoboy.ledger.config

import com.uchuhimo.konf.ConfigSpec

object DatabaseSpec : ConfigSpec() {
    val queueTimeoutMin by required<Long>()
    val queueCheckDelaySec by required<Long>()
}
