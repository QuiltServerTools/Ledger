package com.github.quiltservertools.ledger.utility

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

interface HandlerWithContext {
    var pos: BlockPos?
    fun getPlayer(): ServerPlayer?
    fun onStackChanged(old: ItemStack, new: ItemStack, pos: BlockPos)
}
