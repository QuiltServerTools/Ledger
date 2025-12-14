package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class HandshakeC2SPacket(val nbt: CompoundTag?) : CustomPacketPayload {

    override fun type() = ID

    private fun write(buf: FriendlyByteBuf?) {
        buf?.writeNbt(nbt)
    }

    companion object {
        val ID: CustomPacketPayload.Type<HandshakeC2SPacket> = CustomPacketPayload.Type(LedgerTest.HANDSHAKE)
        val CODEC: StreamCodec<FriendlyByteBuf, HandshakeC2SPacket> =
            CustomPacketPayload.codec(HandshakeC2SPacket::write) { TODO() }
    }

}
