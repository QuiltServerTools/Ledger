package us.potatoboy.ledger.utility

import net.fabricmc.fabric.api.util.NbtType
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.state.State
import net.minecraft.state.StateManager
import net.minecraft.state.property.Property

object NbtUtils {
    fun blockStateToProperties(state: BlockState): CompoundTag? {
        val stateTag = NbtHelper.fromBlockState(state)
        return if (stateTag.contains("Properties", NbtType.COMPOUND)) stateTag.getCompound("Properties") else null
    }

    fun blockStateReadProperties(state: BlockState, tag: CompoundTag) {
        var state = state

        if (tag.contains("Properties", NbtType.COMPOUND)) {
            val propertiesTag = tag.getCompound("Properties")
            val stateManager: StateManager<Block, BlockState> = state.block.stateManager
            val propertyKeys: Iterator<String> = propertiesTag.keys.iterator()
            while (propertyKeys.hasNext()) {
                val propertyKey = propertyKeys.next()
                val property = stateManager.getProperty(propertyKey)
                if (property != null) {
                    val optionalProperty = property.parse(propertiesTag.getString(propertyKey))
                    /*
                    if (optionalProperty.isPresent) {
                        state.with(property, optionalProperty.get() as Comparable)
                    }
                    state = NbtHelper.withProperty(blockState, property, string, compoundTag, tag) as BlockState
                    //TODO fix
                     */
                }
            }
        }
    }
}