package com.github.quiltservertools.ledger.config

import com.github.quiltservertools.ledger.config.util.IdentifierMixin
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.toml
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier

const val CONFIG_PATH = "ledger.toml"

val config: Config = Config {
    addSpec(DatabaseSpec)
    addSpec(SearchSpec)
    addSpec(ActionsSpec)
    addSpec(ColorSpec)
    addSpec(NetworkingSpec)
}
    .apply { this.mapper.addMixIn(Identifier::class.java, IdentifierMixin::class.java) }
    .from.toml.resource(CONFIG_PATH)
    .from.toml.watchFile(FabricLoader.getInstance().configDir.resolve("ledger.toml").toFile())
    .from.env()
    .from.systemProperties()
