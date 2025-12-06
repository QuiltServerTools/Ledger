package com.github.quiltservertools.ledger.actionutils

import net.minecraft.core.BlockPos

interface LocationalInventory {
    fun getLocation(): BlockPos
}
