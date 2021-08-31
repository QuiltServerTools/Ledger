package com.github.quiltservertools.ledger.api

import com.github.quiltservertools.ledger.Ledger
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import org.jetbrains.exposed.sql.Database

class H2Extension : DatabaseExtension {
    override fun getDatabase(server: MinecraftServer): Database =
        Database.connect("jdbc:h2:${server.getSavePath(WorldSavePath.ROOT).resolve("ledger.h2").toFile()};MODE=MySQL", "org.h2.Driver")

    override fun getIdentifier(): Identifier = Ledger.identifier("h2_extension")

    override fun events(events: ExtensionEvents) {
        println("yes")
    }
}
