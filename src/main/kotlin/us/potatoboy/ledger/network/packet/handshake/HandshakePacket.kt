package us.potatoboy.ledger.network.packet.handshake

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.config.NetworkingSpec
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.network.packet.LedgerPacket
import us.potatoboy.ledger.network.packet.LedgerPacketTypes

class HandshakePacket: LedgerPacket<HandshakeContent> {
    override val channel: Identifier = LedgerPacketTypes.HANDSHAKE.id
    override var buf: PacketByteBuf = PacketByteBufs.create()
    override fun populate(content: HandshakeContent) {
        // Ledger information
        // Version
        buf.writeString(FabricLoader.getInstance().getModContainer(Ledger.MOD_ID).get().metadata.version.friendlyString)
        // Is client mod allowed
        var allowed = true
        var reason = 0
        if (config[NetworkingSpec.allowByDefault]) {
            if (config[NetworkingSpec.modBlacklist].contains(content.modid)) {
                // Mod is blacklisted, disallow
                allowed = false
                reason = 1
            }
        } else {
            if (!config[NetworkingSpec.modWhitelist].contains(content.modid)) {
                // Mod is not whitelisted, disallow
                allowed = false
                reason = 2
            }
        }
        buf.writeBoolean(allowed)
        buf.writeShort(reason)
    }
}
