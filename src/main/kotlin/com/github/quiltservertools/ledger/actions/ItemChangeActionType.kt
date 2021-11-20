package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

abstract class ItemChangeActionType : AbstractActionType() {
    override fun getTranslationType(): String {
        val item = Registry.ITEM.get(objectIdentifier)
        return if (item is BlockItem) {
            "block"
        } else {
            "item"
        }
    }

    override fun getObjectMessage(): Text {
        val stack = ItemStack.fromNbt(StringNbtReader.parse(extraData))

        return "${stack.count} ".literal().append(
            TranslatableText(
                Util.createTranslationKey(
                    getTranslationType(),
                    objectIdentifier
                )
            )
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_ITEM,
                    HoverEvent.ItemStackContent(stack)
                )
            )
        }
    }

    protected fun getInventory(world: ServerWorld): Inventory? {
        var inventory: Inventory? = null
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if (block is InventoryProvider) {
            inventory = (block as InventoryProvider).getInventory(blockState, world, pos)
        } else if (world.getBlockEntity(pos) != null) {
            val blockEntity = world.getBlockEntity(pos)!!
            if (blockEntity is Inventory) {
                inventory = blockEntity
                if (inventory is ChestBlockEntity && block is ChestBlock) {
                    inventory = ChestBlock.getInventory(block, blockState, world, pos, true)
                }
            }
        }

        return inventory
    }

    protected fun removeMatchingItem(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val inventory = world?.let { getInventory(it) }

        if (world == null || inventory == null) { return false }

        val rollbackStack = ItemStack.fromNbt(StringNbtReader.parse(extraData))
        val stash: MutableList<Int> = mutableListOf()

        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)

            if (!(stack.isItemEqual(rollbackStack) && stack.nbt == rollbackStack.nbt)) { continue }
            // not the same item so skip might need to check NBT too

            // <0  = reduce rollback stack, add slot to stash and loop
            // >=0 = reduce, remove stashed, return
            when (stack.count - rollbackStack.count) {
                in Int.MIN_VALUE..-1 -> { rollbackStack.count -= stack.count; stash.add(i) }
                in 0..Int.MAX_VALUE  -> { stack.count -= rollbackStack.count
                    stash.forEach { inventory.removeStack(it) }
                    return true }
            }
        }
        return false
    }


    protected fun addItem(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val inventory = world?.let { getInventory(it) }

        if (world == null || inventory == null) { return false }

        val rollbackStack = ItemStack.fromNbt(StringNbtReader.parse(extraData))
        val stash: MutableList<Int> = mutableListOf()

        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)

            if (stack.isEmpty) {
                inventory.setStack(i, rollbackStack)
                stash.forEach {inventory.setStack(it, ItemStack(rollbackStack.item,rollbackStack.maxCount))}
                return true }
            // empty slot so can just add remaining stack and set stashed inv locations to max stack

            if (!(stack.isItemEqual(rollbackStack) && stack.nbt == rollbackStack.nbt) ||
               stack.count == stack.maxCount) { continue }
            // not the same item or full stack so skip

            // >0  = reduce rollback stack, add to slot to stash and loop
            // =<0 = increment final stack, set stashed inv locations to max stack, return
            when (stack.count + rollbackStack.count - stack.maxCount) {
                in 1..Int.MAX_VALUE -> { rollbackStack.count -= stack.maxCount - stack.count; stash.add(i) }
                in Int.MIN_VALUE..0 -> { stack.increment(rollbackStack.count)
                    stash.forEach { inventory.setStack(it, ItemStack(rollbackStack.item,rollbackStack.maxCount)) }
                    return true }
            }
        }
        return false
    }
}
