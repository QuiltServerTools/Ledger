package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class SearchC2SPacket(val query: String) : CustomPayload {

    override fun getId() = ID

    private fun write(buf: PacketByteBuf?) {
        buf?.writeString(query)
    }

    companion object {
        val ID: CustomPayload.Id<SearchC2SPacket> = CustomPayload.Id(LedgerTest.SEARCH)
        val CODEC: PacketCodec<PacketByteBuf, SearchC2SPacket> =
            CustomPayload.codecOf(SearchC2SPacket::write) { TODO() }
    }

}
