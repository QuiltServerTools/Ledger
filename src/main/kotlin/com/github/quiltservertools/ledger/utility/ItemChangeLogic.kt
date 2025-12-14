package com.github.quiltservertools.ledger.utility

import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack

fun addItem(rollbackStack: ItemStack, inventory: Container): Boolean {
    // Check if the inventory has enough space
    var matchingCountLeft = 0
    for (i in 0 until inventory.containerSize) {
        val stack = inventory.getItem(i)
        if (stack.isEmpty) {
            matchingCountLeft += rollbackStack.maxStackSize
        } else if (ItemStack.isSameItemSameComponents(stack, rollbackStack)) {
            matchingCountLeft += stack.maxStackSize - stack.count
        }
    }
    if (matchingCountLeft < rollbackStack.count) {
        return false
    }
    var requiredCount = rollbackStack.count
    for (i in 0 until inventory.containerSize) {
        val stack = inventory.getItem(i)
        if (stack.isEmpty) {
            if (requiredCount > rollbackStack.maxStackSize) {
                inventory.setItem(i, rollbackStack.copyWithCount(rollbackStack.maxStackSize))
                requiredCount -= rollbackStack.maxStackSize
            } else {
                inventory.setItem(i, rollbackStack.copyWithCount(requiredCount))
                requiredCount = 0
            }
        } else if (ItemStack.isSameItemSameComponents(stack, rollbackStack)) {
            val countUntilMax = rollbackStack.maxStackSize - stack.count
            if (requiredCount > countUntilMax) {
                inventory.setItem(i, rollbackStack.copyWithCount(rollbackStack.maxStackSize))
                requiredCount -= countUntilMax
            } else {
                inventory.setItem(i, rollbackStack.copyWithCount(stack.count + requiredCount))
                requiredCount = 0
            }
        }
        if (requiredCount <= 0) {
            return true
        }
    }
    return false
}

fun removeMatchingItem(rollbackStack: ItemStack, inventory: Container): Boolean {
    // Check if the inventory has enough matching items
    var matchingCount = 0
    for (i in 0 until inventory.containerSize) {
        val stack = inventory.getItem(i)
        if (ItemStack.isSameItemSameComponents(stack, rollbackStack)) {
            matchingCount += stack.count
        }
    }
    if (matchingCount < rollbackStack.count) {
        return false
    }
    var requiredCount = rollbackStack.count
    for (i in 0 until inventory.containerSize) {
        val stack = inventory.getItem(i)
        if (ItemStack.isSameItemSameComponents(stack, rollbackStack)) {
            if (requiredCount < stack.count) {
                // Only some parts of this stack are needed
                inventory.setItem(i, stack.copyWithCount(stack.count - requiredCount))
                requiredCount = 0
            } else {
                inventory.setItem(i, ItemStack.EMPTY)
                requiredCount -= stack.count
            }
            if (requiredCount <= 0) {
                return true
            }
        }
    }
    return false
}
