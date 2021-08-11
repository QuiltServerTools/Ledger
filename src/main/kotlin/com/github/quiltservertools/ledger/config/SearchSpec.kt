package com.github.quiltservertools.ledger.config

import com.github.quiltservertools.ledger.commands.CommandConsts
import com.uchuhimo.konf.ConfigSpec

object SearchSpec : ConfigSpec() {
    val pageSize by required<Int>()
    val purgePermissionLevel by optional(CommandConsts.FOUR)
}
