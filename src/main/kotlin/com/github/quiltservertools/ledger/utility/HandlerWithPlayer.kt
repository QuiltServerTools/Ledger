package com.github.quiltservertools.ledger.utility

import net.minecraft.server.network.ServerPlayerEntity

interface HandlerWithPlayer {
    fun getPlayer(): ServerPlayerEntity?
}
