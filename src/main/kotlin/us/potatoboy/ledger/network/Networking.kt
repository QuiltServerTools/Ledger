package us.potatoboy.ledger.network

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import us.potatoboy.ledger.network.packet.Receiver

object Networking {
    fun init() {
        // SHUT UP DETEKT
    }

    private fun register(channel: Identifier, receiver: Receiver) {
        ServerPlayNetworking.registerGlobalReceiver(channel) {
                server: MinecraftServer,
                player: ServerPlayerEntity,
                handler: ServerPlayNetworkHandler,
                buf: PacketByteBuf,
                sender: PacketSender ->
            run {
                receiver.receive(server, player, handler, buf, sender)
            }
        }
    }
}

