package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos

data class InspectC2SPacket(val pos: BlockPos) : CustomPayload {

    override fun getId() = ID

    private fun write(buf: PacketByteBuf?) {
        buf?.writeBlockPos(pos)
    }

    companion object {
        val ID: CustomPayload.Id<InspectC2SPacket> = CustomPayload.Id(LedgerTest.INSPECT)
        val CODEC: PacketCodec<PacketByteBuf, InspectC2SPacket> =
            CustomPayload.codecOf(InspectC2SPacket::write) { TODO() }
    }

}
