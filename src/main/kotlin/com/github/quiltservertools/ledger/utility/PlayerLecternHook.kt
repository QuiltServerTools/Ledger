package com.github.quiltservertools.ledger.utility

import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity

object PlayerLecternHook {
    @JvmStatic
    val activeHandlers = HashMap<PlayerEntity, BlockEntity>()
}
