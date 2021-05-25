package us.potatoboy.ledger.network.packet

import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.actions.ActionType

interface Packet {
    val channel: Identifier
    var buf: PacketByteBuf
    fun populate(action: ActionType)
}
