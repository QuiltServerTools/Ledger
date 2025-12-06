package com.github.quiltservertools.ledger.utility

import com.mojang.logging.LogUtils
import com.mojang.serialization.Dynamic
import net.minecraft.core.HolderGetter
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.ProblemReporter
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput

const val ITEM_NBT_DATA_VERSION = 3817
const val ITEM_COMPONENTS_DATA_VERSION = 3825

const val PROPERTIES = "Properties" // BlockState
const val COUNT_PRE_1_20_5 = "Count" // ItemStack
const val COUNT = "count" // ItemStack
const val UUID = "UUID" // Entity

val LOGGER = LogUtils.getLogger()

object NbtUtils {
    fun blockStateToProperties(state: BlockState): CompoundTag? {
        val stateTag = NbtUtils.writeBlockState(state)
        if (state.block.defaultBlockState() == state) return null // Don't store default block state
        return stateTag.getCompound(PROPERTIES).orElse(null)
    }

    fun blockStateFromProperties(
        tag: CompoundTag,
        name: ResourceLocation,
        blockLookup: HolderGetter<Block>
    ): BlockState {
        val stateTag = CompoundTag()
        stateTag.putString("Name", name.toString())
        stateTag.put(PROPERTIES, tag)
        return NbtUtils.readBlockState(blockLookup, stateTag)
    }

    fun itemFromProperties(tag: String?, name: ResourceLocation, registries: HolderLookup.Provider): ItemStack {
        val extraDataTag = TagParser.parseCompoundFully(tag ?: "{}")
        var itemTag = extraDataTag
        if (!extraDataTag.contains(COUNT)) {
            // 1.20.4 and lower (need data fixing)
            itemTag.putString("id", name.toString())
            if (!itemTag.contains(COUNT_PRE_1_20_5)) {
                // Ledger ItemStack in 1.20.4 and earlier had "Count" omitted if it was 1
                itemTag.putByte(COUNT_PRE_1_20_5, 1)
            }
            itemTag = DataFixers.getDataFixer().update(
                References.ITEM_STACK,
                Dynamic(NbtOps.INSTANCE, itemTag), ITEM_NBT_DATA_VERSION, ITEM_COMPONENTS_DATA_VERSION
            ).cast(NbtOps.INSTANCE) as CompoundTag?
        }
        ProblemReporter.ScopedCollector({ "ledger:itemstack@$name" }, LOGGER).use {
            val readView = TagValueInput.create(it, registries, itemTag)
            return readView.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY)
        }
    }

    fun BlockEntity.createNbt(registries: HolderLookup.Provider): CompoundTag {
        ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)
            .use {
                val writeView = TagValueOutput.createWithContext(it, registries)
                this.saveWithId(writeView)
                return writeView.buildResult()
            }
    }

    fun Entity.createNbt(): CompoundTag {
        ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)
            .use {
                val writeView = TagValueOutput.createWithContext(it, this.registryAccess())
                this.saveWithoutId(writeView)
                return writeView.buildResult()
            }
    }

    fun ItemStack.createNbt(registries: HolderLookup.Provider): CompoundTag {
        ProblemReporter.ScopedCollector({ "ledger:itemstack@${this.item}" }, LOGGER)
            .use {
                val writeView = TagValueOutput.createWithContext(it, registries)
                writeView.store(ItemStack.MAP_CODEC, this)
                return writeView.buildResult()
            }
    }
}
