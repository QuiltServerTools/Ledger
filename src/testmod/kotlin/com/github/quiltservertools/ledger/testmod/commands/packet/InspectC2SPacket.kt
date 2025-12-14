package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

data class InspectC2SPacket(val pos: BlockPos) : CustomPacketPayload {

    override fun type() = ID

    private fun write(buf: FriendlyByteBuf?) {
        buf?.writeBlockPos(pos)
    }

    companion object {
        val ID: CustomPacketPayload.Type<InspectC2SPacket> = CustomPacketPayload.Type(LedgerTest.INSPECT)
        val CODEC: StreamCodec<FriendlyByteBuf, InspectC2SPacket> =
            CustomPacketPayload.codec(InspectC2SPacket::write) { TODO() }
    }

}
