package com.github.quiltservertools.ledger.network.packet.handshake

data class HandshakeContent(val protocolVersion: Int, val ledgerVersion: String, val actions: List<String>)
