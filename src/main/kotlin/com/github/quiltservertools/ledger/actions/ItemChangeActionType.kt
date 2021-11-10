package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback
import com.github.quiltservertools.ledger.utility.Sources
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

        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)

            if (!stack.isItemEqual(rollbackStack)) { continue } // not the same item so skip

            //  0 = remove return // stack matches removal amount
            // <0 = reduce loop   // stack is smaller than rollback amount
            // >0 = reduce return // stack is greater than removal
            when (stack.count - rollbackStack.count) {
                0                    -> {inventory.removeStack(i); return true }
                in Int.MIN_VALUE..-1 -> {rollbackStack.count -= stack.count; inventory.removeStack(i)}
                in 1..Int.MAX_VALUE  -> {stack.count -= rollbackStack.count; return true }
            }
        }
        ItemInsertCallback.EVENT.invoker().insert(rollbackStack, pos, world, Sources.ROLLBACKFAIL, null)
        // would be better if ledger didn't set actions that fail as reverted
        // but would need to be able to undo actions here too.
        return false
    }


    protected fun addItem(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val inventory = world?.let { getInventory(it) }

        if (world == null || inventory == null) { return false }

        val rollbackStack = ItemStack.fromNbt(StringNbtReader.parse(extraData))
        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)


            if (stack.isEmpty) { // empty slot so can just revert to the old state
                inventory.setStack(i, rollbackStack)
                return true
            }

            if (!stack.isItemEqual(rollbackStack) || // not the same item or full so skip
               stack.count == stack.maxCount) { continue }

            //  0 = set max return //stack fits perfectly
            // <0 = increment return // stack won't exceed max so just increment
            // >0 = set max loop // stack can only accept partial amount of items
            when (stack.count + rollbackStack.count - stack.maxCount) {
                0                    -> {stack.count = stack.maxCount; return true }
                in Int.MIN_VALUE..-1 -> {stack.increment(rollbackStack.count); return true }
                in 1..Int.MAX_VALUE  -> {rollbackStack.count -= stack.maxCount - stack.count
                                        stack.count = stack.maxCount }
            }
        }
        ItemRemoveCallback.EVENT.invoker().remove(rollbackStack, pos, world, Sources.ROLLBACKFAIL, null)
        // would be better if ledger didn't set actions that fail as reverted
        // but would need to be able to undo actions here too.
        return false
    }
}
