package us.potatoboy.ledger.config

import com.uchuhimo.konf.ConfigSpec

object NetworkingSpec : ConfigSpec() {
    val networking by required<Boolean>()
    val allowByDefault by required<Boolean>()
    val modWhitelist by required<List<String>>()
    val modBlacklist by required<List<String>>()
}
