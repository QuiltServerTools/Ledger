package com.github.quiltservertools.ledger.network.packet.response

import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class ResponseS2CPacket(val content: ResponseContent) : CustomPacketPayload {
    fun write(buf: FriendlyByteBuf?) {
        // Packet type, rollback response would be `ledger.rollback`
        buf?.writeIdentifier(content.type)
        // Response code
        buf?.writeInt(content.response)
    }

    override fun type() = ID

    companion object {
        val ID: CustomPacketPayload.Type<ResponseS2CPacket> = CustomPacketPayload.Type(LedgerPacketTypes.RESPONSE.id)
        val CODEC: StreamCodec<FriendlyByteBuf, ResponseS2CPacket> = CustomPacketPayload.codec(
            ResponseS2CPacket::write
        ) { _: FriendlyByteBuf? -> TODO() }

        fun sendResponse(content: ResponseContent, sender: PacketSender) {
            sender.sendPacket(ResponseS2CPacket(content))
        }
    }
}
