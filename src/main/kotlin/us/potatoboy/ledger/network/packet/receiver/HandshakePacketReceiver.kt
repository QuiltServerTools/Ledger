package us.potatoboy.ledger.network.packet.receiver

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.commands.CommandConsts
import us.potatoboy.ledger.config.NetworkingSpec
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.network.packet.Receiver
import us.potatoboy.ledger.network.packet.handshake.HandshakeContent
import us.potatoboy.ledger.network.packet.handshake.HandshakePacket

class HandshakePacketReceiver : Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        // This should be sent by the client whenever a player joins with a client mod
        val nbt = buf.readNbt()
        val modid = nbt?.getString("modid")
        val modVersion = nbt?.getString("version")
        Ledger.logger.info("${player.name.asString()} joined the server with a Ledger compatible client mod")
        Ledger.logger.info("Mod: $modid, Version: $modVersion")

        // If player has relevant permissions we add them to the list of
        // network tracked players and send a response packet
        if (Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL)
            && config[NetworkingSpec.networking]) {
            // Player has networking permissions so we send a response
            val packet = HandshakePacket()
            packet.populate(HandshakeContent(modid!!))
            ServerPlayNetworking.send(player, packet.channel, packet.buf)
        }
    }
}
