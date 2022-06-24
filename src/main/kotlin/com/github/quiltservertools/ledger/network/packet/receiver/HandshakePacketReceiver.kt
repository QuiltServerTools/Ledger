package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.logInfo
import com.github.quiltservertools.ledger.network.Networking
import com.github.quiltservertools.ledger.network.Networking.enableNetworking
import com.github.quiltservertools.ledger.network.packet.Receiver
import com.github.quiltservertools.ledger.network.packet.handshake.HandshakeContent
import com.github.quiltservertools.ledger.network.packet.handshake.HandshakePacket
import com.github.quiltservertools.ledger.network.packet.handshake.ModInfo
import com.github.quiltservertools.ledger.registry.ActionRegistry
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.*

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
        // We do some validation on the packet to make sure it's complete and intact
        val info = readInfo(nbt)
        if (info.isPresent) {
            val modid = info.get().modid
            val modVersion = info.get().version
            val ledgerVersion = FabricLoader.getInstance().getModContainer(
                Ledger.MOD_ID).get().metadata.version.friendlyString
            if (Networking.protocolVersion == info.get().protocolVersion) {
                logInfo("${player.name.string} joined the server with a Ledger compatible client mod")
                logInfo("Mod: $modid, Version: $modVersion")

                // Player has networking permissions so we send a response
                val packet = HandshakePacket()
                packet.populate(HandshakeContent(Networking.protocolVersion, ledgerVersion, ActionRegistry.getTypes().toList()))
                ServerPlayNetworking.send(player, packet.channel, packet.buf)
                player.enableNetworking()
            } else {
                player.sendMessage(
                    Text.translatable(
                        "text.ledger.network.protocols_mismatched",
                        Networking.protocolVersion, info.get().protocolVersion
                    ), false
                )
                logInfo("${player.name.string} joined the server with a Ledger compatible client mod, " +
                        "but has a mismatched protocol: Ledger protocol version: ${Networking.protocolVersion}" +
                        ", Client mod protocol version ${info.get().protocolVersion}")
            }
        } else {
            player.sendMessage(Text.translatable("text.ledger.network.no_mod_info"), false)
        }
    }

    private fun readInfo(nbt: NbtCompound?): Optional<ModInfo> {
        if (nbt == null) {
            return Optional.empty()
        }

        return Optional.of(ModInfo(nbt.getString("modid"), nbt.getString("version"), nbt.getInt("protocol_version")))
    }
}
