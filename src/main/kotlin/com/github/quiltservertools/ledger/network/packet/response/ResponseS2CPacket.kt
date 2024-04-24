package com.github.quiltservertools.ledger.network.packet.response

import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class ResponseS2CPacket(val content: ResponseContent) : CustomPayload {
    fun write(buf: PacketByteBuf?) {
        // Packet type, rollback response would be `ledger.rollback`
        buf?.writeIdentifier(content.type)
        // Response code
        buf?.writeInt(content.response)
    }

    override fun getId() = ID

    companion object {
        val ID: CustomPayload.Id<ResponseS2CPacket> = CustomPayload.Id(LedgerPacketTypes.RESPONSE.id)
        val CODEC: PacketCodec<PacketByteBuf, ResponseS2CPacket> = CustomPayload.codecOf(
            ResponseS2CPacket::write
        ) { _: PacketByteBuf? -> TODO() }

        fun sendResponse(content: ResponseContent, sender: PacketSender) {
            sender.sendPacket(ResponseS2CPacket(content))
        }
    }
}
