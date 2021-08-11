package com.github.quiltservertools.ledger.network.packet.action

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.network.packet.LedgerPacket
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

class ActionPacket: LedgerPacket<ActionType> {
    override val channel: Identifier = LedgerPacketTypes.ACTION.id
    override var buf: PacketByteBuf = PacketByteBufs.create()

    override fun populate(content: ActionType) {
        // Position
        buf.writeBlockPos(content.pos)
        // Type
        buf.writeString(content.identifier)
        // Dimension
        buf.writeIdentifier(content.world)
        // Objects
        buf.writeIdentifier(content.oldObjectIdentifier)
        buf.writeIdentifier(content.objectIdentifier)
        // Source
        buf.writeString(content.sourceProfile?.name ?: "@" + content.sourceName)
        // Epoch second of event, sent as a long
        buf.writeLong(content.timestamp.epochSecond)
        // Has been rolled back?
        buf.writeBoolean(content.rolledBack)
        // NBT
        buf.writeString(content.extraData ?: "")
    }
}
