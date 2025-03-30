package com.github.quiltservertools.ledger.utility

import net.minecraft.item.ItemStack
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos

interface HandlerWithContext {
    var pos: BlockPos?
    fun getPlayer(): ServerPlayerEntity?
    fun onStackChanged(old: ItemStack, new: ItemStack, pos: BlockPos)
}
