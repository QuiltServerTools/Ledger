package us.potatoboy.ledger.network.packet.handshake

data class HandshakeContent(val allowed: Boolean, val protocolVersion: Int)
