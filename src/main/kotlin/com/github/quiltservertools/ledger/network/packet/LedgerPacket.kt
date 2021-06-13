package com.github.quiltservertools.ledger.network.packet

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

interface LedgerPacket<T> {
    val channel: Identifier
    var buf: PacketByteBuf
    fun populate(content: T)
}
