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
import net.minecraft.server.level.ServerPlayer

object Networking {
    // List of players who have a compatible client mod
    private var networkedPlayers = mutableSetOf<ServerPlayer>()
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

    fun ServerPlayer.hasNetworking() = networkedPlayers.contains(this)

    fun ServerPlayer.enableNetworking() = networkedPlayers.add(this)

    fun ServerPlayer.disableNetworking() = networkedPlayers.remove(this)
}
