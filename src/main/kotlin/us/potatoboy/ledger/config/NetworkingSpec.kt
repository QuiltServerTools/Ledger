package us.potatoboy.ledger.config

import com.uchuhimo.konf.ConfigSpec

object NetworkingSpec: ConfigSpec() {
    val networking by optional(false)
    val allowByDefault by optional(true)
    val modWhitelist by optional<List<String>>(ArrayList())
    val modBlacklist by optional<List<String>>(ArrayList())
}
