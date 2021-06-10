package us.potatoboy.ledger.network.packet.receiver

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.commands.CommandConsts
import us.potatoboy.ledger.logInfo
import us.potatoboy.ledger.network.Networking
import us.potatoboy.ledger.network.Networking.enableNetworking
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
        if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL)) return
        // This should be sent by the client whenever a player joins with a client mod
        val nbt = buf.readNbt()
        val modid = nbt?.getString("modid")
        val modVersion = nbt?.getString("version")
        val protocolVersion = nbt?.getInt("protocol_version")
        if (Networking.protocolVersion == protocolVersion) {
            logInfo("${player.name.asString()} joined the server with a Ledger compatible client mod")
            logInfo("Mod: $modid, Version: $modVersion")

            // Player has networking permissions so we send a response
            val packet = HandshakePacket()
            packet.populate(HandshakeContent(Networking.isAllowed(modid!!), Networking.protocolVersion))
            ServerPlayNetworking.send(player, packet.channel, packet.buf)
            player.enableNetworking()
        } else {
            player.sendMessage(
                TranslatableText(
                    "text.ledger.network.protocols_mismatched",
                    Networking.protocolVersion, protocolVersion
                ), false
            )
        }
    }
}
