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

    /**
     * Reoptimization: dictionary-encoded block states (state string <-> block_states.id).
     * Keeps the actions table rows narrow (int ref instead of a full state string per row).
     */
    val blockStateKeys: BiMap<String, Int> = HashBiMap.create()
}
