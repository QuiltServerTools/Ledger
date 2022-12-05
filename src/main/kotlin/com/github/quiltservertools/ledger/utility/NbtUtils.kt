package com.github.quiltservertools.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

private const val PROPERTIES = "Properties"

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
}
