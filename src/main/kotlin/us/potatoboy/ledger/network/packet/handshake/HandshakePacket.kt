package us.potatoboy.ledger.network.packet.handshake

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.network.packet.LedgerPacket
import us.potatoboy.ledger.network.packet.LedgerPacketTypes

class HandshakePacket: LedgerPacket<HandshakeContent> {
    override val channel: Identifier = LedgerPacketTypes.HANDSHAKE.id
    override var buf: PacketByteBuf = PacketByteBufs.create()
    override fun populate(content: HandshakeContent) {
        // Ledger information
        // Version
        buf.writeInt(content.protocolVersion)
        // Is client mod allowed
        val allowed = content.allowed
        buf.writeBoolean(allowed)
    }
}
