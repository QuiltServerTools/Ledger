package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class HandshakeC2SPacket(val nbt: NbtCompound?) : CustomPayload {

    override fun getId() = ID

    private fun write(buf: PacketByteBuf?) {
        buf?.writeNbt(nbt)
    }

    companion object {
        val ID: CustomPayload.Id<HandshakeC2SPacket> = CustomPayload.Id(LedgerTest.HANDSHAKE)
        val CODEC: PacketCodec<PacketByteBuf, HandshakeC2SPacket> =
            CustomPayload.codecOf(HandshakeC2SPacket::write) { TODO() }
    }

}
