package com.github.quiltservertools.ledger.network.packet.handshake

import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class HandshakeS2CPacket(val content: HandshakeContent) : CustomPacketPayload {
    fun write(buf: FriendlyByteBuf?) {
        // Ledger information
        // Protocol Version
        buf?.writeInt(content.protocolVersion)

        // Ledger Version
        buf?.writeUtf(content.ledgerVersion)

        // We tell the client mod how many actions we are writing
        buf?.writeInt(content.actions.size)

        for (action in content.actions) {
            buf?.writeUtf(action)
        }
    }

    override fun type() = ID

    companion object {
        val ID: CustomPacketPayload.Type<HandshakeS2CPacket> = CustomPacketPayload.Type(LedgerPacketTypes.HANDSHAKE.id)
        val CODEC: StreamCodec<FriendlyByteBuf, HandshakeS2CPacket> = CustomPacketPayload.codec(
            HandshakeS2CPacket::write
        ) { _: FriendlyByteBuf? -> TODO() }
    }
}
