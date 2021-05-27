package us.potatoboy.ledger.network.packet

import net.minecraft.util.Identifier
import us.potatoboy.ledger.Ledger

enum class PacketTypes(val id: Identifier) {
    ACTION(Ledger.identifier("action")),
    INSPECT(Ledger.identifier("inspect")),
    SEARCH(Ledger.identifier("search"))
}
