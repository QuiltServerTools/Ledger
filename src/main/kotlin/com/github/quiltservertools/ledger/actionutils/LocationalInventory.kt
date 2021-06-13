package com.github.quiltservertools.ledger.actionutils

import net.minecraft.util.math.BlockPos

interface LocationalInventory {
    fun getLocation(): BlockPos
}
