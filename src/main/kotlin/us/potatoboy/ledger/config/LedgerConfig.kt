package us.potatoboy.ledger.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml

const val CONFIG_PATH = "ledger.toml"

val config = Config {
    addSpec(DatabaseSpec)
    addSpec(SearchSpec)
    addSpec(ActionsSpec)
}
    .from.toml.resource(CONFIG_PATH)
    .from.toml.watchFile("config\\$CONFIG_PATH")
    .from.env()
    .from.systemProperties()
