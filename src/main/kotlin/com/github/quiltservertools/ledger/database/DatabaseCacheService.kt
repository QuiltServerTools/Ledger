package com.github.quiltservertools.ledger.database

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import net.minecraft.util.Identifier
import java.util.UUID

object DatabaseCacheService {
    val actionIdentifierKeys: Cache<String, Int> = CacheBuilder.newBuilder().build()

    val worldIdentifierKeys: Cache<Identifier, Int> = CacheBuilder.newBuilder().build()

    val objectIdentifierKeys: Cache<Identifier, Int> = CacheBuilder.newBuilder().build()

    val sourceKeys: Cache<String, Int> = CacheBuilder.newBuilder().build()

    val playerKeys: Cache<UUID, Int> = CacheBuilder.newBuilder().build()
}
