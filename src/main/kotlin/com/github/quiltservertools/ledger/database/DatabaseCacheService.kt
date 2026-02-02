package com.github.quiltservertools.ledger.database

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.resources.Identifier
import java.util.*

object DatabaseCacheService {
    val actionIdentifierKeys: BiMap<String, Int> = HashBiMap.create()

    val worldIdentifierKeys: BiMap<Identifier, Int> = HashBiMap.create()

    val objectIdentifierKeys: BiMap<Identifier, Int> = HashBiMap.create()

    val sourceKeys: BiMap<String, Int> = HashBiMap.create()

    val playerKeys: BiMap<UUID, Int> = HashBiMap.create()

    val playernameKeys: BiMap<String, Int> = HashBiMap.create()
}
