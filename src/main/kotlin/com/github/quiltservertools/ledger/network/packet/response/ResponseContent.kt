package com.github.quiltservertools.ledger.network.packet.response

import net.minecraft.resources.ResourceLocation

data class ResponseContent(val type: ResourceLocation, val response: Int)
