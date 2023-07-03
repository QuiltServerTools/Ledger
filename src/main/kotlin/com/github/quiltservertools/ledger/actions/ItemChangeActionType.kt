package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.LecternBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.LecternBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.AliasedBlockItem
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util

abstract class ItemChangeActionType : AbstractActionType() {
    override fun getTranslationType(): String {
        val item = Registries.ITEM.get(objectIdentifier)
        return if (item is BlockItem && item !is AliasedBlockItem) {
            "block"
        } else {
            "item"
        }
    }

    override fun getObjectMessage(): Text {
        val stack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

        return "${stack.count} ".literal().append(
            Text.translatable(
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

    private fun getInventory(world: ServerWorld): Inventory? {
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

        if (world != null) {
            val rollbackStack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

            if (inventory != null) {
                for (i in 0 until inventory.size()) {
                    val stack = inventory.getStack(i)
                    if (ItemStack.areItemsEqual(stack, rollbackStack)) {
                        inventory.setStack(i, ItemStack.EMPTY)
                        return true
                    }
                }
            } else if (rollbackStack.isOf(Items.WRITABLE_BOOK) || rollbackStack.isOf(Items.WRITTEN_BOOK)) {
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is LecternBlockEntity) {
                    blockEntity.book = ItemStack.EMPTY
                    LecternBlock.setHasBook(null, world, pos, blockEntity.cachedState, false)
                    return true
                }
            }
        }

        return false
    }

    protected fun addItem(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        val inventory = world?.let { getInventory(it) }

        if (world != null) {
            val rollbackStack = NbtUtils.itemFromProperties(extraData, objectIdentifier)

            if (inventory != null) {
                for (i in 0 until inventory.size()) {
                    val stack = inventory.getStack(i)
                    if (stack.isEmpty) {
                        inventory.setStack(i, rollbackStack)
                        return true
                    }
                }
            } else if (rollbackStack.isOf(Items.WRITABLE_BOOK) || rollbackStack.isOf(Items.WRITTEN_BOOK)) {
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is LecternBlockEntity && !blockEntity.hasBook()) {
                    blockEntity.book = rollbackStack
                    LecternBlock.setHasBook(null, world, pos, blockEntity.cachedState, true)
                    return true
                }
            }
        }

        return false
    }
}
