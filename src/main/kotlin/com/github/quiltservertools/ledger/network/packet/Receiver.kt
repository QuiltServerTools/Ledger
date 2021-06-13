package com.github.quiltservertools.ledger.network.packet

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity

interface Receiver {
    fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    )
}
