package com.github.quiltservertools.ledger.network

import com.github.quiltservertools.ledger.config.NetworkingSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.network.packet.receiver.HandshakeC2SPacket
import com.github.quiltservertools.ledger.network.packet.receiver.InspectC2SPacket
import com.github.quiltservertools.ledger.network.packet.receiver.PurgeC2SPacket
import com.github.quiltservertools.ledger.network.packet.receiver.RollbackC2SPacket
import com.github.quiltservertools.ledger.network.packet.receiver.SearchC2SPacket
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity

object Networking {
    // List of players who have a compatible client mod
    private var networkedPlayers = mutableSetOf<ServerPlayerEntity>()
    const val PROTOCOL_VERSION = 3

    init {
        if (config[NetworkingSpec.networking]) {
            PayloadTypeRegistry.playC2S().register(InspectC2SPacket.ID, InspectC2SPacket.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(InspectC2SPacket.ID, InspectC2SPacket)

            PayloadTypeRegistry.playC2S().register(SearchC2SPacket.ID, SearchC2SPacket.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(SearchC2SPacket.ID, SearchC2SPacket)

            PayloadTypeRegistry.playC2S().register(HandshakeC2SPacket.ID, HandshakeC2SPacket.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(HandshakeC2SPacket.ID, HandshakeC2SPacket)

            PayloadTypeRegistry.playC2S().register(RollbackC2SPacket.ID, RollbackC2SPacket.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(RollbackC2SPacket.ID, RollbackC2SPacket)

            PayloadTypeRegistry.playC2S().register(PurgeC2SPacket.ID, PurgeC2SPacket.CODEC)
            ServerPlayNetworking.registerGlobalReceiver(PurgeC2SPacket.ID, PurgeC2SPacket)
        }
    }

    fun ServerPlayerEntity.hasNetworking() = networkedPlayers.contains(this)

    fun ServerPlayerEntity.enableNetworking() = networkedPlayers.add(this)

    fun ServerPlayerEntity.disableNetworking() = networkedPlayers.remove(this)
}
