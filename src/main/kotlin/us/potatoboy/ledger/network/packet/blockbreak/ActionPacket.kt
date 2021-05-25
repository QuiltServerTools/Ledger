package us.potatoboy.ledger.network.packet.blockbreak

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.block.Blocks
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.network.packet.Packet
import us.potatoboy.ledger.network.packet.PacketTypes

class ActionPacket: Packet {
    override val channel: Identifier = PacketTypes.ACTION.id
    override var buf: PacketByteBuf = PacketByteBufs.create()

    override fun populate(action: ActionType) {
        // Position
        buf.writeBlockPos(action.pos)
        // Dimension
        buf.writeIdentifier(action.world)
        // Checks if the block was added or removed
        val added = action.blockState?.block != Blocks.AIR.defaultState
        buf.writeBoolean(added)
        // Write only the significant BlockPos
        if (added) {
            buf.writeString(action.blockState.toString())
        } else {
            buf.writeString(action.oldBlockState.toString())
        }
        buf.writeString(if (action.sourceProfile != null) action.sourceProfile!!.name else action.sourceName)
        buf.writeString(if (action.extraData != null) action.extraData.toString() else "")

        println(buf.toString())
    }
}
