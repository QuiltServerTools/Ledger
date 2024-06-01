package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import java.time.Instant
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

data class ActionS2CPacket(
    val pos: BlockPos,
    val id: String,
    val world: Identifier,
    val oldObjectId: Identifier,
    val objectId: Identifier,
    val source: String,
    val timestamp: Instant,
    val extraData: String
) :
    CustomPayload {

    override fun getId() = ID

    companion object : ClientPlayNetworking.PlayPayloadHandler<ActionS2CPacket> {
        val ID: CustomPayload.Id<ActionS2CPacket> = CustomPayload.Id(LedgerTest.ACTION)
        val CODEC: PacketCodec<PacketByteBuf, ActionS2CPacket> =
            CustomPayload.codecOf({ _, _ -> TODO() }, {
                ActionS2CPacket(
                    it.readBlockPos(),
                    it.readString(),
                    it.readIdentifier(),
                    it.readIdentifier(),
                    it.readIdentifier(),
                    it.readString(),
                    Instant.ofEpochSecond(it.readLong()),
                    it.readString()
                )
            })

        override fun receive(payload: ActionS2CPacket, context: ClientPlayNetworking.Context?) {
            LedgerTest.LOGGER.info(
                "pos={}, id={}, world={}, oldObjectId={}, objectId={}, source={}, timestamp={}, extraData={}",
                payload.pos,
                payload.id,
                payload.world,
                payload.oldObjectId,
                payload.objectId,
                payload.source,
                payload.timestamp,
                payload.extraData
            )
        }
    }

}
