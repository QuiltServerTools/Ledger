package com.github.quiltservertools.ledger.utility

import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtHelper
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
        return if (stateTag.contains(
                PROPERTIES,
                NbtElement.COMPOUND_TYPE.toInt()
            )
        ) {
                stateTag.getCompound(PROPERTIES)
            } else {
                null
            }
    }

    fun blockStateFromProperties(tag: NbtCompound, name: Identifier): BlockState {
        val stateTag = NbtCompound()
        stateTag.putString("Name", name.toString())
        stateTag.put(PROPERTIES, tag)
        return NbtHelper.toBlockState(Registries.BLOCK.readOnlyWrapper, stateTag)
    }
}
