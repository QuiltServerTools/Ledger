package com.github.quiltservertools.ledger.actionutils

import net.minecraft.world.Container

interface DoubleInventoryHelper {
    fun getInventory(slot: Int): Container
}
