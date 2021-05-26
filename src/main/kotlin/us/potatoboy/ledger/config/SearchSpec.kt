package us.potatoboy.ledger.config

import com.uchuhimo.konf.ConfigSpec

object SearchSpec : ConfigSpec() {
    val pageSize by required<Int>()
}
