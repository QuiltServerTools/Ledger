package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.addItem
import com.github.quiltservertools.ledger.utility.getOtherChestSide
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import com.github.quiltservertools.ledger.utility.removeMatchingItem
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.LecternBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.LecternBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

abstract class ItemChangeActionType : AbstractActionType() {
    // Not used
    override fun getTranslationType(): String = "item"

    private fun getStack(server: MinecraftServer) = NbtUtils.itemFromProperties(
        extraData,
        objectIdentifier,
        server.registryManager
    )

    override fun getObjectMessage(source: ServerCommandSource): Text {
        val stack = getStack(source.server)

        return "${stack.count} ".literal().append(
            stack.itemName
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent.ShowItem(
                    stack
                )
            )
        }
    }

    protected fun previewItemChange(preview: Preview, player: ServerPlayerEntity, insert: Boolean) {
        val world = player.entityWorld.server.getWorld(world)
        val state = world?.getBlockState(pos)
        state?.isOf(Blocks.CHEST)?.let {
            if (it) {
                val otherPos = getOtherChestSide(state, pos)
                if (otherPos != null) {
                    addPreview(preview, player, otherPos, insert)
                }
            }
        }
        addPreview(preview, player, pos, insert)
    }

    private fun addPreview(preview: Preview, player: ServerPlayerEntity, pos: BlockPos, insert: Boolean) {
        preview.modifiedItems.compute(pos) { _, list ->
            list ?: mutableListOf()
        }?.add(Pair(getStack(player.entityWorld.server), insert))
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
            val rollbackStack = getStack(server)
            if (inventory != null) {
                return removeMatchingItem(rollbackStack, inventory)
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
            val rollbackStack = getStack(server)
            if (inventory != null) {
                return addItem(rollbackStack, inventory)
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
