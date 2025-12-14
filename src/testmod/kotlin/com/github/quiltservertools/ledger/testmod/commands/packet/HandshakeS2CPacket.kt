package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class HandshakeS2CPacket(val protocolVersion: Int, val ledgerVersion: String, val actionTypes: Collection<String>) :
    CustomPacketPayload {

    override fun type() = ID

    companion object : ClientPlayNetworking.PlayPayloadHandler<HandshakeS2CPacket> {
        val ID: CustomPacketPayload.Type<HandshakeS2CPacket> = CustomPacketPayload.Type(LedgerTest.HANDSHAKE)
        val CODEC: StreamCodec<FriendlyByteBuf, HandshakeS2CPacket> =
            CustomPacketPayload.codec({ _, _ -> TODO() }, {
                val protocolVersion = it.readInt()
                val ledgerVersion = it.readUtf()
                val actionsLength = it.readInt()
                val actionTypes: MutableList<String> = mutableListOf()
                for (i in 0..actionsLength) {
                    actionTypes.add(it.readUtf())
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
