package com.github.quiltservertools.ledger.api

import net.minecraft.server.MinecraftServer
import org.jetbrains.exposed.sql.Database

interface DatabaseExtension : LedgerExtension {
    fun getDatabase(server: MinecraftServer): Database
}
