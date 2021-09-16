package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import net.fabricmc.loader.api.FabricLoader

const val CONFIG_PATH = "ledger.toml"

val config: Config = Config {
    addSpec(DatabaseSpec)
    addSpec(SearchSpec)
    addSpec(ActionsSpec)
    addSpec(ColorSpec)
    addSpec(NetworkingSpec)
}
    .from.toml.resource(CONFIG_PATH)
    .from.toml.watchFile(FabricLoader.getInstance().configDir.resolve("ledger.toml").toFile())
    .from.env()
    .from.systemProperties()
