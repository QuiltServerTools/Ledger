package com.github.quiltservertools.ledger.network.packet.action

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.CustomPayload.Id

data class ActionS2CPacket(val content: ActionType) : CustomPayload {
    private fun write(buf: PacketByteBuf?) {
        // Position
        buf?.writeBlockPos(content.pos)
        // Type
        buf?.writeString(content.identifier)
        // Dimension
        buf?.writeIdentifier(content.world)
        // Objects
        buf?.writeIdentifier(content.oldObjectIdentifier)
        buf?.writeIdentifier(content.objectIdentifier)
        // Source
        buf?.writeString(content.sourceProfile?.name ?: "@" + content.sourceName)
        // Epoch second of event, sent as a long
        buf?.writeLong(content.timestamp.epochSecond)
        // Has been rolled back?
        buf?.writeBoolean(content.rolledBack)
        // NBT
        buf?.writeString(content.extraData ?: "")
    }

    override fun getId() = ID

    companion object {
        val ID: Id<ActionS2CPacket> = Id(LedgerPacketTypes.ACTION.id)
        val CODEC: PacketCodec<PacketByteBuf, ActionS2CPacket> = CustomPayload.codecOf(
            ActionS2CPacket::write
        ) { _: PacketByteBuf? -> TODO() }
    }
}
