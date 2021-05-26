package us.potatoboy.ledger.network.packet

import net.minecraft.util.Identifier

enum class PacketTypes(val id: Identifier) {
    ACTION(Identifier("ledger", "action")),
    INSPECT(Identifier("ledger", "inspect"))
}
