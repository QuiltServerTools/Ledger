package us.potatoboy.ledger.actions

import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.HoverEvent
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry
import us.potatoboy.ledger.TextColorPallet

class ItemRemoveActionType : ItemChangeActionType() {
    override val identifier: String = "item-remove"

    override fun rollback(world: ServerWorld): Boolean {
        return getInventory(world)?.let { addItem(it) } ?: false
    }

    override fun restore(world: ServerWorld): Boolean {
        return getInventory(world)?.let { removeMatchingItem(it) } ?: false
    }
}