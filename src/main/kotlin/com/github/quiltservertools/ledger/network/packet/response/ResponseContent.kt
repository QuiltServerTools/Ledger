package com.github.quiltservertools.ledger.network.packet.response

import net.minecraft.resources.Identifier

data class ResponseContent(val type: Identifier, val response: Int)
