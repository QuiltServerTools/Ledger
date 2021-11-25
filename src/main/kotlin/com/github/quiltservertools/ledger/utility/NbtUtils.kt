package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.EntityKillActionType
import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier

const val PROPERTIES = "Properties" // BlockState
const val COUNT = "Count" // ItemStack
const val TAG = "tag" // ItemStack
const val UUID = "UUID" // Entity
const val ITEMROTATION = "ItemRotation" // DecorationEntity
const val ARMORITEMS = "ArmorItems" // Entity
const val HANDITEMS = "HandItems" // Entity
const val ITEM = "Item" // DecorationEntity
const val ROTATION = "Rotation" // Entity
const val FACING = "Facing" // DecorationEntity
const val POSE = "Pose" // ArmorStandEntity

object NbtUtils {
    fun blockStateToProperties(state: BlockState): NbtCompound? {
        if (state.block.defaultState == state) return null // Don't store default block state

        val stateTag = NbtHelper.fromBlockState(state)
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

        if (item.count > 1) { itemTag.putByte(COUNT, item.count.toByte()) }
        if (item.nbt != null) { itemTag.put(TAG, item.nbt) }

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

        if (tagNbt.contains(TAG)) { itemTag.put(TAG, tagNbt.getCompound(TAG)) }

        return ItemStack.fromNbt(itemTag)
    }

    //idk at some point need to look into what is needed here. surely we dont need every single nbt to be logged
    fun entityToProperties(entity: Entity, actionType: String): NbtCompound? {
        val entityTag = NbtCompound()
        val storedNBT = entity.writeNbt(NbtCompound())

        if (storedNBT.contains(UUID)) entityTag.putUuid(UUID, storedNBT.getUuid(UUID))
        if (storedNBT.contains(ITEMROTATION)) entityTag.putByte(ITEMROTATION, storedNBT.getByte(ITEMROTATION))
        Ledger.logger.info(storedNBT)

        if (actionType == EntityKillActionType().identifier) { // add this to spawn too // needs some way to skip over defaults
            if (storedNBT.contains(ROTATION)) entityTag.put(ROTATION, storedNBT.get(ROTATION))
            if (storedNBT.contains(POSE)) entityTag.put(POSE, storedNBT.get(POSE))
            if (storedNBT.contains(HANDITEMS)) entityTag.put(HANDITEMS, storedNBT.get(HANDITEMS))
            if (storedNBT.contains(ARMORITEMS)) entityTag.put(ARMORITEMS, storedNBT.get(ARMORITEMS))
            if (storedNBT.contains(ITEM)) entityTag.put(ITEM, storedNBT.get(ITEM))
            if (storedNBT.contains(FACING)) entityTag.putByte(FACING, storedNBT.getByte(FACING))
        }


        return if (entityTag.isEmpty) null else entityTag
    }

    fun entityFromProperties(tag: String?): NbtCompound? {
        val entityTag = NbtCompound()

        if (tag == null) { return null }

        val storedNBT = StringNbtReader.parse(tag)

        if (storedNBT.contains(UUID)) entityTag.putUuid(UUID, storedNBT.getUuid(UUID))
        if (storedNBT.contains(ITEMROTATION)) entityTag.putByte(ITEMROTATION, storedNBT.getByte(ITEMROTATION))
        if (storedNBT.contains(ROTATION)) entityTag.put(ROTATION, storedNBT.get(ROTATION))
        if (storedNBT.contains(POSE)) entityTag.put(POSE, storedNBT.get(POSE))
        if (storedNBT.contains(HANDITEMS)) entityTag.put(HANDITEMS, storedNBT.getCompound(HANDITEMS))
        if (storedNBT.contains(ARMORITEMS)) entityTag.put(ARMORITEMS, storedNBT.getCompound(ARMORITEMS))
        if (storedNBT.contains(ITEM)) entityTag.put(ITEM, storedNBT.get(ITEM))
        if (storedNBT.contains(FACING)) entityTag.putByte(FACING, storedNBT.getByte(FACING))


        return entityTag
    }
}
