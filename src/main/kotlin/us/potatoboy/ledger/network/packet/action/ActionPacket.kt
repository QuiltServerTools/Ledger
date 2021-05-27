package us.potatoboy.ledger.network.packet.action

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.network.packet.LedgerPacket
import us.potatoboy.ledger.network.packet.LedgerPacketTypes

class ActionPacket: LedgerPacket {
    override val channel: Identifier = LedgerPacketTypes.ACTION.id
    override var buf: PacketByteBuf = PacketByteBufs.create()

    override fun populate(action: ActionType) {
        // Position
        buf.writeBlockPos(action.pos)
        // Type
        buf.writeString(action.identifier)
        // Dimension
        buf.writeIdentifier(action.world)
        // Objects
        buf.writeIdentifier(action.oldObjectIdentifier)
        buf.writeIdentifier(action.objectIdentifier)
        // Source
        buf.writeString(action.sourceProfile?.name ?: "@" + action.sourceName)
        // Epoch second of event, sent as a long
        buf.writeLong(action.timestamp.epochSecond)
        // NBT
        buf.writeString(action.extraData ?: "")
    }
}
