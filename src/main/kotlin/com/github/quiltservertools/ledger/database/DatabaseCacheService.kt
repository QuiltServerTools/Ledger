package com.github.quiltservertools.ledger.database

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.resources.ResourceLocation
import java.util.UUID

object DatabaseCacheService {
    val actionIdentifierKeys: BiMap<String, Int> = HashBiMap.create()

    val worldIdentifierKeys: BiMap<ResourceLocation, Int> = HashBiMap.create()

    val objectIdentifierKeys: BiMap<ResourceLocation, Int> = HashBiMap.create()

    val sourceKeys: BiMap<String, Int> = HashBiMap.create()

    val playerKeys: BiMap<UUID, Int> = HashBiMap.create()
}
