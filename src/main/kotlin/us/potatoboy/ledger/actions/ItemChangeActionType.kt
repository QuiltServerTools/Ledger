package us.potatoboy.ledger.actions

import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.literal

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
        val stack = ItemStack.fromTag(StringNbtReader.parse(extraData))

        return "${stack.count} ".literal().append(
            TranslatableText(
                Util.createTranslationKey(
                    getTranslationType(),
                    objectIdentifier
                )
            )
        ).setStyle(TextColorPallet.tertiary).styled {
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
        } else if (block.hasBlockEntity()) {
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

    protected fun removeMatchingItem(inventory: Inventory): Boolean {
        val rollbackStack = ItemStack.fromTag(StringNbtReader.parse(extraData))

        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)
            if (stack.isItemEqual(rollbackStack)) {
                inventory.setStack(i, ItemStack.EMPTY)
                return true
            }
        }

        return false
    }

    protected fun addItem(inventory: Inventory): Boolean {
        val rollbackStack = ItemStack.fromTag(StringNbtReader.parse(extraData))

        for (i in 0 until inventory.size()) {
            val stack = inventory.getStack(i)
            if (stack.isEmpty) {
                inventory.setStack(i, rollbackStack)
                return true
            }
        }

        return false
    }
}
