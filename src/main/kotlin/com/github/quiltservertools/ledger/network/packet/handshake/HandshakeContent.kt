package com.github.quiltservertools.ledger.network.packet.handshake

data class HandshakeContent(val allowed: Boolean, val protocolVersion: Int, val ledgerVersion: String)
