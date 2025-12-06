package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class SearchC2SPacket(val query: String) : CustomPacketPayload {

    override fun type() = ID

    private fun write(buf: FriendlyByteBuf?) {
        buf?.writeUtf(query)
    }

    companion object {
        val ID: CustomPacketPayload.Type<SearchC2SPacket> = CustomPacketPayload.Type(LedgerTest.SEARCH)
        val CODEC: StreamCodec<FriendlyByteBuf, SearchC2SPacket> =
            CustomPacketPayload.codec(SearchC2SPacket::write) { TODO() }
    }

}
