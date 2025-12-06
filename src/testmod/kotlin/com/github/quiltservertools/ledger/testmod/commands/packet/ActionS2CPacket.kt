package com.github.quiltservertools.ledger.testmod.commands.packet


import com.github.quiltservertools.ledger.testmod.LedgerTest
import java.time.Instant
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.BlockPos

data class ActionS2CPacket(
    val pos: BlockPos,
    val id: String,
    val world: ResourceLocation,
    val oldObjectId: ResourceLocation,
    val objectId: ResourceLocation,
    val source: String,
    val timestamp: Instant,
    val extraData: String
) :
    CustomPacketPayload {

    override fun type() = ID

    companion object : ClientPlayNetworking.PlayPayloadHandler<ActionS2CPacket> {
        val ID: CustomPacketPayload.Type<ActionS2CPacket> = CustomPacketPayload.Type(LedgerTest.ACTION)
        val CODEC: StreamCodec<FriendlyByteBuf, ActionS2CPacket> =
            CustomPacketPayload.codec({ _, _ -> TODO() }, {
                ActionS2CPacket(
                    it.readBlockPos(),
                    it.readUtf(),
                    it.readResourceLocation(),
                    it.readResourceLocation(),
                    it.readResourceLocation(),
                    it.readUtf(),
                    Instant.ofEpochSecond(it.readLong()),
                    it.readUtf()
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
