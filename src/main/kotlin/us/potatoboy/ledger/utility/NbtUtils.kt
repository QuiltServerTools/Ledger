package us.potatoboy.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.util.Identifier

const val PROPERTIES = "Properties"
object NbtUtils {
    fun blockStateToProperties(state: BlockState): CompoundTag? {
        val stateTag = NbtHelper.fromBlockState(state)
        if (state.block.defaultState == state) return null // Don't store default block state
        return if (stateTag.contains(PROPERTIES, NbtType.COMPOUND)) stateTag.getCompound(PROPERTIES) else null
    }

    fun blockStateFromProperties(tag: CompoundTag, name: Identifier): BlockState {
        val stateTag = CompoundTag()
        stateTag.putString("Name", name.toString())
        stateTag.put(PROPERTIES, tag)

        return NbtHelper.toBlockState(stateTag)
    }
}
