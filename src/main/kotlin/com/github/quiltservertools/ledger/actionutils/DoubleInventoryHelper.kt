package com.github.quiltservertools.ledger.actionutils

import net.minecraft.inventory.Inventory

interface DoubleInventoryHelper {
    fun getInventory(slot: Int): Inventory
}
