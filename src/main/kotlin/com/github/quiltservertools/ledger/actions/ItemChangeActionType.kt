package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.addItem
import com.github.quiltservertools.ledger.utility.getOtherChestSide
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import com.github.quiltservertools.ledger.utility.removeMatchingItem
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.LecternBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity

abstract class ItemChangeActionType : AbstractActionType() {
    // Not used
    override fun getTranslationType(): String = "item"

    private fun getStack(server: MinecraftServer) = NbtUtils.itemFromProperties(
        extraData,
        objectIdentifier,
        server.registryAccess()
    )

    override fun getObjectMessage(source: CommandSourceStack): Component {
        val stack = getStack(source.server)

        return "${stack.count} ".literal().append(
            stack.itemName
        ).setStyle(TextColorPallet.secondaryVariant).withStyle {
            it.withHoverEvent(
                HoverEvent.ShowItem(
                    stack
                )
            )
        }
    }

    protected fun previewItemChange(preview: Preview, player: ServerPlayer, insert: Boolean) {
        val world = player.level().server.getWorld(world)
        val state = world?.getBlockState(pos)
        state?.`is`(Blocks.CHEST)?.let {
            if (it) {
                val otherPos = getOtherChestSide(state, pos)
                if (otherPos != null) {
                    addPreview(preview, player, otherPos, insert)
                }
            }
        }
        addPreview(preview, player, pos, insert)
    }

    private fun addPreview(preview: Preview, player: ServerPlayer, pos: BlockPos, insert: Boolean) {
        preview.modifiedItems.compute(pos) { _, list ->
            list ?: mutableListOf()
        }?.add(Pair(getStack(player.level().server), insert))
    }

    private fun getInventory(world: ServerLevel): Container? {
        var inventory: Container? = null
        val blockState = world.getBlockState(pos)
        val block = blockState.block
        if (block is WorldlyContainerHolder) {
            inventory = (block as WorldlyContainerHolder).getContainer(blockState, world, pos)
        } else if (world.getBlockEntity(pos) != null) {
            val blockEntity = world.getBlockEntity(pos)!!
            if (blockEntity is Container) {
                inventory = blockEntity
                if (inventory is ChestBlockEntity && block is ChestBlock) {
                    inventory = ChestBlock.getContainer(block, blockState, world, pos, true)
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
            } else if (rollbackStack.`is`(Items.WRITABLE_BOOK) || rollbackStack.`is`(Items.WRITTEN_BOOK)) {
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is LecternBlockEntity) {
                    blockEntity.book = ItemStack.EMPTY
                    LecternBlock.resetBookState(null, world, pos, blockEntity.blockState, false)
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
            } else if (rollbackStack.`is`(Items.WRITABLE_BOOK) || rollbackStack.`is`(Items.WRITTEN_BOOK)) {
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is LecternBlockEntity && !blockEntity.hasBook()) {
                    blockEntity.book = rollbackStack
                    LecternBlock.resetBookState(null, world, pos, blockEntity.blockState, true)
                    return true
                }
            }
        }

        return false
    }
}
