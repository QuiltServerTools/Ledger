package com.github.quiltservertools.ledger.config

import com.uchuhimo.konf.ConfigSpec

object SearchSpec : ConfigSpec() {
    val pageSize by required<Int>()
    val purgePermissionLevel by required<Int>()
}
