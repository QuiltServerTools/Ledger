package us.potatoboy.ledger.network

import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import us.potatoboy.ledger.network.packet.LedgerPacketTypes
import us.potatoboy.ledger.network.packet.Receiver
import us.potatoboy.ledger.network.packet.receiver.InspectReceiver
import us.potatoboy.ledger.network.packet.receiver.SearchReceiver


fun registerNetworking() {
    register(LedgerPacketTypes.INSPECT.id, InspectReceiver())
    register(LedgerPacketTypes.SEARCH.id, SearchReceiver())
}

private fun register(channel: Identifier, receiver: Receiver) {
    ServerPlayNetworking.registerGlobalReceiver(channel) {
            server: MinecraftServer,
            player: ServerPlayerEntity,
            handler: ServerPlayNetworkHandler,
            buf: PacketByteBuf,
            sender: PacketSender ->
        receiver.receive(server, player, handler, buf, sender)
    }
}

