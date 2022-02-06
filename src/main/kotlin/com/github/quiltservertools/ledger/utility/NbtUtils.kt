package com.github.quiltservertools.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Identifier

const val PROPERTIES = "Properties" // BlockState
const val COUNT = "Count" // ItemStack
const val TAG = "tag" // ItemStack
const val UUID = "UUID" // Entity
//const val COLOR = "Color" // SheepEntity
//const val COLLARCOLOR = "CollarColor" // WolfEntity
//const val SHEARED = "Sheared" // SheepEntity
//const val PUMPKIN = "Pumpkin" // SheepEntity

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
        if (item.count > 1) { itemTag.putByte(COUNT, item.count.toByte()) }
        // don't log the item count if there is only 1 item. the log itself indicates there must be at least 1
        if (item.nbt != null) { itemTag.put(TAG, item.nbt) }

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
        if (tagNbt.contains(COUNT)) { itemTag.putByte(COUNT, tagNbt.getByte(COUNT)) }
        else { itemTag.putByte(COUNT, 1) }
        // item was missing count tag. if it's been logged then it must have a single item

        if (tagNbt.contains(TAG)) { itemTag.put(TAG, tagNbt.getCompound(TAG)) }

        return ItemStack.fromNbt(itemTag)
    }

//    fun entityToProperties(entity: Entity, sourceType: String): String? {
//        val entityTag = NbtCompound()
//        val storedNBT = entity.writeNbt(NbtCompound())
//
//        entityTag.putUuid(UUID, storedNBT.getUuid(UUID))
//
//        if (sourceType == Sources.DYE && entity is SheepEntity){
//            entityTag.putByte(COLOR, storedNBT.getByte(COLOR))
//        }
//        if (sourceType == Sources.SHEAR && entity is SheepEntity){
//            entityTag.putByte(COLOR, storedNBT.getByte(COLOR)) // i think this is needed
//            entityTag.putBoolean(SHEARED, storedNBT.getBoolean(SHEARED))
//        }
//        if (sourceType == Sources.SHEAR && entity is SnowGolemEntity){
//            entityTag.putBoolean(PUMPKIN, storedNBT.getBoolean(PUMPKIN))
//        }
//        if (sourceType == Sources.DYE && entity is WolfEntity || entity is CatEntity){
//            entityTag.putByte(COLLARCOLOR, storedNBT.getByte(COLLARCOLOR))
//        }
//
//        return if (entityTag.isEmpty) null else entityTag.toString()
//    }


}
