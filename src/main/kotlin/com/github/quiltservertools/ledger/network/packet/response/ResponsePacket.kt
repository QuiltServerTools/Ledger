package com.github.quiltservertools.ledger.network.packet.response

import com.github.quiltservertools.ledger.network.packet.LedgerPacket
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

class ResponsePacket : LedgerPacket<ResponseContent> {
    override var buf: PacketByteBuf = PacketByteBufs.create()
    override val channel: Identifier = LedgerPacketTypes.RESPONSE.id
    override fun populate(content: ResponseContent) {
        // Packet type, rollback response would be `ledger.rollback`
        buf.writeIdentifier(content.type)
        // Response code
        buf.writeInt(content.response)
    }

    companion object {
        fun sendResponse(content: ResponseContent, sender: PacketSender) {
            val response = ResponsePacket()
            response.populate(content)
            sender.sendPacket(LedgerPacketTypes.RESPONSE.id, response.buf)
        }
    }
}
