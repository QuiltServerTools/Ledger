package com.github.quiltservertools.ledger.utility

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity

object PlayerLecternHook {
    @JvmStatic
    val activeHandlers = HashMap<Player, BlockEntity>()
}
