package us.potatoboy.ledger.utility

import net.minecraft.server.network.ServerPlayerEntity

interface HandlerWithPlayer {
    fun getPlayer(): ServerPlayerEntity?
}
