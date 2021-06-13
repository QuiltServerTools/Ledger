package com.github.quiltservertools.ledger.network.packet

import net.minecraft.util.Identifier
import com.github.quiltservertools.ledger.Ledger

enum class LedgerPacketTypes(val id: Identifier) {
    ACTION(Ledger.identifier("action")),
    INSPECT(Ledger.identifier("inspect")),
    SEARCH(Ledger.identifier("search")),
    HANDSHAKE(Ledger.identifier("handshake"))
}
