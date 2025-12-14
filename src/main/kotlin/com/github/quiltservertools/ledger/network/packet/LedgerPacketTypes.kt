package com.github.quiltservertools.ledger.network.packet

import com.github.quiltservertools.ledger.Ledger
import net.minecraft.resources.Identifier

enum class LedgerPacketTypes(val id: Identifier) {
    ACTION(Ledger.identifier("action")),
    INSPECT_POS(Ledger.identifier("inspect")),
    SEARCH(Ledger.identifier("search")),
    HANDSHAKE(Ledger.identifier("handshake")),
    RESPONSE(Ledger.identifier("response")),
    ROLLBACK(Ledger.identifier("rollback")),
    PURGE(Ledger.identifier("purge")),
}
