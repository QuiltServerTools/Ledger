package us.potatoboy.ledger.network.packet.action

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.network.packet.LedgerPacket
import us.potatoboy.ledger.network.packet.LedgerPacketTypes

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
        // NBT
        buf.writeString(content.extraData ?: "")
    }
}
