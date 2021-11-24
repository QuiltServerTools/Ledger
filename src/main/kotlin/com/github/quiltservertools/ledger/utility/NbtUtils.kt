package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier
import java.util.*

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

        return NbtHelper.toBlockState(stateTag)
    }

    fun itemToProperties(item: ItemStack): NbtCompound? {
        val itemTag = NbtCompound()

        val a = item.writeNbt(NbtCompound())

        Ledger.logger.info(a)

        if (item.count > 1) itemTag.putByte(COUNT, item.count.toByte())
        if (item.nbt != null) itemTag.put(TAG, item.nbt)

        return if (itemTag.isEmpty) null else itemTag
    }

    fun itemFromProperties(tag: String?, name: Identifier): ItemStack {
        val itemTag = NbtCompound()

        itemTag.putString("id", name.toString())

        if (tag == null) {
            itemTag.putByte(COUNT, 1);
            return ItemStack.fromNbt(itemTag)
        }

        val tagNbt = StringNbtReader.parse(tag)

        if (tagNbt.contains(COUNT)) { itemTag.putByte(COUNT, tagNbt.getByte(COUNT)) }
        else { itemTag.putByte(COUNT, 1) }

        if (tagNbt.contains(TAG)) itemTag.put(TAG, tagNbt.getCompound(TAG))

        return ItemStack.fromNbt(itemTag)
    }
//idk at some point need to look into what is needed here. surely we dont need every single nbt to be logged
    fun entityUUIDToProperties(entity: Entity): NbtCompound? {
        val entityTag = NbtCompound()

        if (entity.uuid != null) entityTag.putUuid(UUID, entity.uuid)

        return if (entityTag.isEmpty) null else entityTag
    }

    fun entityUUIDFromProperties(tag: NbtCompound) = if (!tag.contains(UUID)) null else tag.getUuid(UUID)
}
