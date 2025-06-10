package com.github.quiltservertools.ledger.database

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.util.Identifier
import java.util.UUID

object DatabaseCacheService {
    val actionIdentifierKeys: BiMap<String, Int> = HashBiMap.create()

    val worldIdentifierKeys: BiMap<Identifier, Int> = HashBiMap.create()

    val objectIdentifierKeys: BiMap<Identifier, Int> = HashBiMap.create()

    val sourceKeys: BiMap<String, Int> = HashBiMap.create()

    val playerKeys: BiMap<UUID, Int> = HashBiMap.create()
}
