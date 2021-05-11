package us.potatoboy.ledger.actionutils

import net.minecraft.inventory.Inventory

interface DoubleInventoryHelper {
    fun getInventory(slot: Int): Inventory
}
