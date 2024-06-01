package com.github.quiltservertools.ledger.config

import com.github.quiltservertools.ledger.Ledger
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import net.minecraft.util.WorldSavePath
import java.nio.file.Path

@Suppress("MagicNumber")
object DatabaseSpec : ConfigSpec() {
    val queueTimeoutMin by required<Long>()
    val queueCheckDelaySec by required<Long>()
    val autoPurgeDays by required<Int>()
    val batchSize by optional<Int>(1000)
    val batchDelay by optional<Int>(10)
    val logSQL by optional<Boolean>(false)
    val location by optional<String?>(null)
}

fun Config.getDatabasePath(): Path {
    val location = config[DatabaseSpec.location]
    return if (location != null) {
        Path.of(location)
    } else {
        Ledger.server.getSavePath(WorldSavePath.ROOT)
    }
}
