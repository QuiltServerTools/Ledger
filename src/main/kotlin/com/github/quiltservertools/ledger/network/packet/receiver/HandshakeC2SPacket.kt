package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.logInfo
import com.github.quiltservertools.ledger.network.Networking
import com.github.quiltservertools.ledger.network.Networking.enableNetworking
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import com.github.quiltservertools.ledger.network.packet.handshake.HandshakeContent
import com.github.quiltservertools.ledger.network.packet.handshake.ModInfo
import com.github.quiltservertools.ledger.registry.ActionRegistry
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import java.util.*

data class HandshakeC2SPacket(val nbt: CompoundTag?) : CustomPacketPayload {

    override fun type() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<HandshakeC2SPacket> {
        val ID: CustomPacketPayload.Type<HandshakeC2SPacket> = CustomPacketPayload.Type(LedgerPacketTypes.HANDSHAKE.id)
        val CODEC: StreamCodec<FriendlyByteBuf, HandshakeC2SPacket> = CustomPacketPayload.codec({ _, _ -> TODO() }, {
            HandshakeC2SPacket(it.readNbt())
        })

        override fun receive(payload: HandshakeC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL)) return
            // This should be sent by the client whenever a player joins with a client mod
            // We do some validation on the packet to make sure it's complete and intact
            val info = readInfo(payload.nbt)
            if (info.isPresent) {
                val modid = info.get().modid
                val modVersion = info.get().version
                val ledgerVersion = FabricLoader.getInstance().getModContainer(
                    Ledger.MOD_ID
                ).get().metadata.version.friendlyString
                if (Networking.PROTOCOL_VERSION == info.get().protocolVersion) {
                    logInfo("${player.name.string} joined the server with a Ledger compatible client mod")
                    logInfo("Mod: $modid, Version: $modVersion")

                    // Player has networking permissions so we send a response
                    val packet = com.github.quiltservertools.ledger.network.packet.handshake.HandshakeS2CPacket(
                        HandshakeContent(
                            Networking.PROTOCOL_VERSION,
                            ledgerVersion,
                            ActionRegistry.getTypes().toList()
                        )
                    )
                    ServerPlayNetworking.send(player, packet)
                    player.enableNetworking()
                } else {
                    player.displayClientMessage(
                        Component.translatable(
                            "text.ledger.network.protocols_mismatched",
                            Networking.PROTOCOL_VERSION,
                            info.get().protocolVersion
                        ),
                            false
                    )
                    logInfo(
                        "${player.name.string} joined the server with a Ledger compatible client mod, " +
                            "but has a mismatched protocol: Ledger protocol version: ${Networking.PROTOCOL_VERSION}" +
                            ", Client mod protocol version ${info.get().protocolVersion}"
                    )
                }
            } else {
                player.displayClientMessage(Component.translatable("text.ledger.network.no_mod_info"), false)
            }
        }
        private fun readInfo(nbt: CompoundTag?): Optional<ModInfo> {
            if (nbt == null) {
                return Optional.empty()
            }

            return Optional.of(
                ModInfo(
                    nbt.getString("modid").orElseThrow(),
                    nbt.getString("version").orElseThrow(),
                    nbt.getInt("protocol_version").orElseThrow()
                )
            )
        }
    }
}
