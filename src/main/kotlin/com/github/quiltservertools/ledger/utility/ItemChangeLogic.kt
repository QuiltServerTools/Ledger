package com.github.quiltservertools.ledger.utility

import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack

fun addItem(rollbackStack: ItemStack, inventory: Inventory): Boolean {
    // Check if the inventory has enough space
    var matchingCountLeft = 0
    for (i in 0 until inventory.size()) {
        val stack = inventory.getStack(i)
        if (stack.isEmpty) {
            matchingCountLeft += rollbackStack.maxCount
        } else if (ItemStack.areItemsAndComponentsEqual(stack, rollbackStack)) {
            matchingCountLeft += stack.maxCount - stack.count
        }
    }
    if (matchingCountLeft < rollbackStack.count) {
        return false
    }
    var requiredCount = rollbackStack.count
    for (i in 0 until inventory.size()) {
        val stack = inventory.getStack(i)
        if (stack.isEmpty) {
            if (requiredCount > rollbackStack.maxCount) {
                inventory.setStack(i, rollbackStack.copyWithCount(rollbackStack.maxCount))
                requiredCount -= rollbackStack.maxCount
            } else {
                inventory.setStack(i, rollbackStack.copyWithCount(requiredCount))
                requiredCount = 0
            }
        } else if (ItemStack.areItemsAndComponentsEqual(stack, rollbackStack)) {
            val countUntilMax = rollbackStack.maxCount - stack.count
            if (requiredCount > countUntilMax) {
                inventory.setStack(i, rollbackStack.copyWithCount(rollbackStack.maxCount))
                requiredCount -= countUntilMax
            } else {
                inventory.setStack(i, rollbackStack.copyWithCount(stack.count + requiredCount))
                requiredCount = 0
            }
        }
        if (requiredCount <= 0) {
            return true
        }
    }
    return false
}

fun removeMatchingItem(rollbackStack: ItemStack, inventory: Inventory): Boolean {
    // Check if the inventory has enough matching items
    var matchingCount = 0
    for (i in 0 until inventory.size()) {
        val stack = inventory.getStack(i)
        if (ItemStack.areItemsAndComponentsEqual(stack, rollbackStack)) {
            matchingCount += stack.count
        }
    }
    if (matchingCount < rollbackStack.count) {
        return false
    }
    var requiredCount = rollbackStack.count
    for (i in 0 until inventory.size()) {
        val stack = inventory.getStack(i)
        if (ItemStack.areItemsAndComponentsEqual(stack, rollbackStack)) {
            if (requiredCount < stack.count) {
                // Only some parts of this stack are needed
                inventory.setStack(i, stack.copyWithCount(stack.count - requiredCount))
                requiredCount = 0
            } else {
                inventory.setStack(i, ItemStack.EMPTY)
                requiredCount -= stack.count
            }
            if (requiredCount <= 0) {
                return true
            }
        }
    }
    return false
}
