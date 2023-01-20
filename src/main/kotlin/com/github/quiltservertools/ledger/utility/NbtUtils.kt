package com.github.quiltservertools.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

const val PROPERTIES = "Properties" // BlockState
const val COUNT = "Count" // ItemStack
const val TAG = "tag" // ItemStack
const val UUID = "UUID" // Entity

object NbtUtils {
    fun blockStateToProperties(state: BlockState): NbtCompound? {
        val stateTag = NbtHelper.fromBlockState(state)
        if (state.block.defaultState == state) return null // Don't store default block state
        return if (stateTag.contains(PROPERTIES, NbtType.COMPOUND)) stateTag.getCompound(PROPERTIES) else null
    }

    fun blockStateFromProperties(tag: NbtCompound, name: Identifier): BlockState {
        val stateTag = NbtCompound()
        stateTag.putString("Name", name.toString())
        stateTag.put(PROPERTIES, tag)
        return NbtHelper.toBlockState(Registries.BLOCK.readOnlyWrapper, stateTag)
    }

    fun itemToProperties(item: ItemStack): NbtCompound? {
        val itemTag = NbtCompound()

        // Don't log the item count if there is only 1 item. The log itself indicates there must be at least 1
        if (item.count > 1) {
            itemTag.putByte(COUNT, item.count.toByte())
        }

        if (item.nbt != null) {
            itemTag.put(TAG, item.nbt)
        }
        return if (itemTag.isEmpty) null else itemTag
    }

    fun itemFromProperties(tag: String?, name: Identifier): ItemStack {
        val itemTag = NbtCompound()

        itemTag.putString("id", name.toString())
        if (tag == null) {
            itemTag.putByte(COUNT, 1)
            return ItemStack.fromNbt(itemTag)
        }

        val tagNbt = StringNbtReader.parse(tag)
        // Item was missing count tag. If it's been logged, it must have a single item
        if (tagNbt.contains(COUNT)) {
            itemTag.putByte(COUNT, tagNbt.getByte(COUNT))
        } else {
            itemTag.putByte(COUNT, 1)
        }

        if (tagNbt.contains(TAG)) {
            itemTag.put(TAG, tagNbt.getCompound(TAG))
        }

        return ItemStack.fromNbt(itemTag)
    }
}
