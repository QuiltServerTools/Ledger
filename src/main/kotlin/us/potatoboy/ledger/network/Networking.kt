package us.potatoboy.ledger.network

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import us.potatoboy.ledger.config.NetworkingSpec
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.network.packet.LedgerPacketTypes
import us.potatoboy.ledger.network.packet.Receiver
import us.potatoboy.ledger.network.packet.receiver.HandshakePacketReceiver
import us.potatoboy.ledger.network.packet.receiver.InspectReceiver
import us.potatoboy.ledger.network.packet.receiver.SearchReceiver

object Networking {
    // List of players who have a compatible client mod
    var networkedPlayers: MutableList<ServerPlayerEntity> = ArrayList()
    const val protocolVersion: Int = 0

    init {
        if (config[NetworkingSpec.networking]) {
            register(LedgerPacketTypes.INSPECT.id, InspectReceiver())
            register(LedgerPacketTypes.SEARCH.id, SearchReceiver())
            register(LedgerPacketTypes.HANDSHAKE.id, HandshakePacketReceiver())
            ServerPlayConnectionEvents.DISCONNECT.register { h: ServerPlayNetworkHandler, _: MinecraftServer ->
                run {
                    networkedPlayers.removeIf { p: ServerPlayerEntity -> p == h.player }
                }
            }
        }
    }

    private fun register(channel: Identifier, receiver: Receiver) {
        ServerPlayNetworking.registerGlobalReceiver(channel) { server: MinecraftServer,
                                                               player: ServerPlayerEntity,
                                                               handler: ServerPlayNetworkHandler,
                                                               buf: PacketByteBuf,
                                                               sender: PacketSender ->
            receiver.receive(server, player, handler, buf, sender)
        }
    }

    fun isAllowed(modid: String): Boolean {
        var allowed = true
        if (config[NetworkingSpec.allowByDefault]) {
            if (config[NetworkingSpec.modBlacklist].contains(modid)) {
                // Mod is blacklisted, disallow
                allowed = false
            }
        } else {
            if (!config[NetworkingSpec.modWhitelist].contains(modid)) {
                // Mod is not whitelisted, disallow
                allowed = false
            }
        }
        return allowed
    }
}

