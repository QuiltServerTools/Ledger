package us.potatoboy.ledger.network.packet

import net.minecraft.util.Identifier
import us.potatoboy.ledger.Ledger

enum class LedgerPacketTypes(val id: Identifier) {
    ACTION(Ledger.identifier("action")),
    INSPECT(Ledger.identifier("inspect")),
    SEARCH(Ledger.identifier("search")),
    HANDSHAKE(Ledger.identifier("handshake"))
}
