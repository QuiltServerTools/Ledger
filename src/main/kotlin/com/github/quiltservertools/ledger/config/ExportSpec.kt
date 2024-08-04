package com.github.quiltservertools.ledger.config

import com.github.quiltservertools.ledger.Ledger
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import net.minecraft.util.WorldSavePath
import java.nio.file.Path

object ExportSpec : ConfigSpec() {
    val location by optional<String?>("")
    val format by optional<String?>("csv")
}

fun Config.getExportDir(): Path {
    val location = config[ExportSpec.location]
    return if (!location.isNullOrEmpty()) {
        Path.of(location)
    } else {
        Ledger.server.getSavePath(WorldSavePath.ROOT).resolve("data")
    }.normalize()
}
