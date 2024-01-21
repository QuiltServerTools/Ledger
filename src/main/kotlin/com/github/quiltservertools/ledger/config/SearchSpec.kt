package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec
import java.time.ZoneId

object SearchSpec : ConfigSpec() {
    val pageSize by required<Int>()
    val purgePermissionLevel by required<Int>()
    val timeZone by required<ZoneId>()
}
