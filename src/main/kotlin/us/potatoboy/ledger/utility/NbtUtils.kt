package us.potatoboy.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.state.State
import net.minecraft.state.StateManager
import net.minecraft.state.property.Property
import net.minecraft.util.Identifier

object NbtUtils {
    fun blockStateToProperties(state: BlockState): CompoundTag? {
        val stateTag = NbtHelper.fromBlockState(state)
        if (state.block.defaultState == state) return null //Don't store default block state
        return if (stateTag.contains("Properties", NbtType.COMPOUND)) stateTag.getCompound("Properties") else null
    }

    fun blockStateFromProperties(tag: CompoundTag, name: Identifier): BlockState {
        val stateTag = CompoundTag();
        stateTag.putString("Name", name.toString());
        stateTag.put("Properties", tag)

        return NbtHelper.toBlockState(stateTag)
    }
}