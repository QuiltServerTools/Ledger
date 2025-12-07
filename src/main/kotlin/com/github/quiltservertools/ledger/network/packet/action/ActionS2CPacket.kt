package com.github.quiltservertools.ledger.network.packet.action

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type

data class ActionS2CPacket(val content: ActionType) : CustomPacketPayload {
    private fun write(buf: FriendlyByteBuf?) {
        // Position
        buf?.writeBlockPos(content.pos)
        // Type
        buf?.writeUtf(content.identifier)
        // Dimension
        buf?.writeIdentifier(content.world!!)
        // Objects
        buf?.writeIdentifier(content.oldObjectIdentifier)
        buf?.writeIdentifier(content.objectIdentifier)
        // Source
        buf?.writeUtf(content.sourceProfile?.name ?: ("@" + content.sourceName))
        // Epoch second of event, sent as a long
        buf?.writeLong(content.timestamp.epochSecond)
        // Has been rolled back?
        buf?.writeBoolean(content.rolledBack)
        // NBT
        buf?.writeUtf(content.extraData ?: "")
    }

    override fun type() = ID

    companion object {
        val ID: Type<ActionS2CPacket> = Type(LedgerPacketTypes.ACTION.id)
        val CODEC: StreamCodec<FriendlyByteBuf, ActionS2CPacket> = CustomPacketPayload.codec(
            ActionS2CPacket::write
        ) { _: FriendlyByteBuf? -> TODO() }
    }
}
