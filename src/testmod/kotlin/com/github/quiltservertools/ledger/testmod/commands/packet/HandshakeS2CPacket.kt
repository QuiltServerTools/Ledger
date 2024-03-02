package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class HandshakeS2CPacket(val protocolVersion: Int, val ledgerVersion: String, val actionTypes: Collection<String>) :
    CustomPayload {

    override fun getId() = ID

    companion object : ClientPlayNetworking.PlayPayloadHandler<HandshakeS2CPacket> {
        val ID: CustomPayload.Id<HandshakeS2CPacket> = CustomPayload.Id(LedgerTest.HANDSHAKE)
        val CODEC: PacketCodec<PacketByteBuf, HandshakeS2CPacket> =
            CustomPayload.codecOf({ _, _ -> TODO() }, {
                val protocolVersion = it.readInt()
                val ledgerVersion = it.readString()
                val actionsLength = it.readInt()
                val actionTypes: MutableList<String> = mutableListOf()
                for (i in 0..actionsLength) {
                    actionTypes.add(it.readString())
                }
                HandshakeS2CPacket(protocolVersion, ledgerVersion, actionTypes)
            })

        override fun receive(payload: HandshakeS2CPacket, context: ClientPlayNetworking.Context?) {
            LedgerTest.LOGGER.info("Protocol version: {}", payload.protocolVersion)
            LedgerTest.LOGGER.info("Ledger version: {}", payload.ledgerVersion)
            LedgerTest.LOGGER.info("Number of types registered: {}", payload.actionTypes.size)
            payload.actionTypes.forEach {
                LedgerTest.LOGGER.info("Action type: {}", it)
            }
        }
    }

}
