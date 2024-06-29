package com.github.quiltservertools.ledger.utility

import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

interface HandlerWithContext {
    fun getPlayer(): ServerPlayerEntity?
    fun getPos(): BlockPos?
    fun setPos(pos: BlockPos)
    fun onStackChanged(old: ItemStack, new: ItemStack, pos: BlockPos)
}
