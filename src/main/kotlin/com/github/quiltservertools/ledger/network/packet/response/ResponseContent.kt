package com.github.quiltservertools.ledger.network.packet.response

import net.minecraft.util.Identifier

data class ResponseContent(val type: Identifier, val response: Int)
