package com.github.quiltservertools.ledger.database

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import net.minecraft.resources.ResourceLocation
import java.util.*

object DatabaseCacheService {
    val actionResourceLocationKeys: BiMap<String, Int> = HashBiMap.create()

    val worldResourceLocationKeys: BiMap<ResourceLocation, Int> = HashBiMap.create()

    val objectResourceLocationKeys: BiMap<ResourceLocation, Int> = HashBiMap.create()

    val sourceKeys: BiMap<String, Int> = HashBiMap.create()

    val playerKeys: BiMap<UUID, Int> = HashBiMap.create()

    val playernameKeys: BiMap<String, Int> = HashBiMap.create()
}
