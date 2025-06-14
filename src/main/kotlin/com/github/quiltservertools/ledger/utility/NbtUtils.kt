package com.github.quiltservertools.ledger.utility

import com.mojang.logging.LogUtils
import com.mojang.serialization.Dynamic
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.datafixer.Schemas
import net.minecraft.datafixer.TypeReferences
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.RegistryEntryLookup
import net.minecraft.registry.RegistryWrapper
import net.minecraft.storage.NbtReadView
import net.minecraft.storage.NbtWriteView
import net.minecraft.util.ErrorReporter
import net.minecraft.util.Identifier

const val ITEM_NBT_DATA_VERSION = 3817
const val ITEM_COMPONENTS_DATA_VERSION = 3825

const val PROPERTIES = "Properties" // BlockState
const val COUNT_PRE_1_20_5 = "Count" // ItemStack
const val COUNT = "count" // ItemStack
const val UUID = "UUID" // Entity

val LOGGER = LogUtils.getLogger()

object NbtUtils {
    fun blockStateToProperties(state: BlockState): NbtCompound? {
        val stateTag = NbtHelper.fromBlockState(state)
        if (state.block.defaultState == state) return null // Don't store default block state
        return stateTag.getCompound(PROPERTIES).orElse(null)
    }

    fun blockStateFromProperties(
        tag: NbtCompound,
        name: Identifier,
        blockLookup: RegistryEntryLookup<Block>
    ): BlockState {
        val stateTag = NbtCompound()
        stateTag.putString("Name", name.toString())
        stateTag.put(PROPERTIES, tag)
        return NbtHelper.toBlockState(blockLookup, stateTag)
    }

    fun itemFromProperties(tag: String?, name: Identifier, registries: RegistryWrapper.WrapperLookup): ItemStack {
        val extraDataTag = StringNbtReader.readCompound(tag ?: "{}")
        var itemTag = extraDataTag
        if (!extraDataTag.contains(COUNT)) {
            // 1.20.4 and lower (need data fixing)
            itemTag.putString("id", name.toString())
            if (!itemTag.contains(COUNT_PRE_1_20_5)) {
                // Ledger ItemStack in 1.20.4 and earlier had "Count" omitted if it was 1
                itemTag.putByte(COUNT_PRE_1_20_5, 1)
            }
            itemTag = Schemas.getFixer().update(
                TypeReferences.ITEM_STACK,
                Dynamic(NbtOps.INSTANCE, itemTag), ITEM_NBT_DATA_VERSION, ITEM_COMPONENTS_DATA_VERSION
            ).cast(NbtOps.INSTANCE) as NbtCompound?
        }
        ErrorReporter.Logging({ "ledger:itemstack@$name" }, LOGGER).use {
            val readView = NbtReadView.create(it, registries, itemTag)
            return readView.read(ItemStack.MAP_CODEC).orElse(ItemStack.EMPTY)
        }
    }

    fun BlockEntity.createNbt(registries: RegistryWrapper.WrapperLookup): NbtCompound {
        ErrorReporter.Logging(this.reporterContext, LOGGER)
            .use {
                val writeView = NbtWriteView.create(it, registries)
                this.writeDataWithId(writeView)
                return writeView.nbt
            }
    }

    fun Entity.createNbt(): NbtCompound {
        ErrorReporter.Logging(this.errorReporterContext, LOGGER)
            .use {
                val writeView = NbtWriteView.create(it, this.registryManager)
                this.writeData(writeView)
                return writeView.nbt
            }
    }

    fun ItemStack.createNbt(registries: RegistryWrapper.WrapperLookup): NbtCompound {
        ErrorReporter.Logging({ "ledger:itemstack@${this.item}" }, LOGGER)
            .use {
                val writeView = NbtWriteView.create(it, registries)
                writeView.put(ItemStack.MAP_CODEC, this)
                return writeView.nbt
            }
    }
}
