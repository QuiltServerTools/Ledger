package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import com.github.quiltservertools.ledger.network.packet.action.ActionS2CPacket
import com.github.quiltservertools.ledger.network.packet.response.ResponseCodes
import com.github.quiltservertools.ledger.network.packet.response.ResponseContent
import com.github.quiltservertools.ledger.network.packet.response.ResponseS2CPacket
import com.github.quiltservertools.ledger.utility.getInspectResults
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class PurgeC2SPacket(val pos: BlockPos, val pages: Int) : CustomPacketPayload {

    override fun type() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<PurgeC2SPacket> {
        val ID: CustomPacketPayload.Type<PurgeC2SPacket> = CustomPacketPayload.Type(LedgerPacketTypes.PURGE.id)
        val CODEC: StreamCodec<FriendlyByteBuf, PurgeC2SPacket> = CustomPacketPayload.codec({ _, _ -> TODO() }, {
            PurgeC2SPacket(it.readBlockPos(), it.readInt())
        })

        override fun receive(payload: PurgeC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            val sender = context.responseSender()
            if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
                !Permissions.check(player, "ledger.commands.inspect", CommandConsts.PERMISSION_LEVEL)
            ) {
                ResponseS2CPacket.sendResponse(
                    ResponseContent(
                        LedgerPacketTypes.INSPECT_POS.id,
                        ResponseCodes.NO_PERMISSION.code
                    ),
                    sender
                )
                return
            }
            ResponseS2CPacket.sendResponse(
                ResponseContent(LedgerPacketTypes.INSPECT_POS.id, ResponseCodes.EXECUTING.code),
                sender
            )

            Ledger.launch {
                val results = player.getInspectResults(payload.pos)
                for (i in 1..payload.pages) {
                    val page = DatabaseManager.searchActions(results.searchParams, i)
                    page.actions.forEach { action ->
                        sender.sendPacket(ActionS2CPacket(action))
                    }
                }
                ResponseS2CPacket.sendResponse(
                    ResponseContent(LedgerPacketTypes.INSPECT_POS.id, ResponseCodes.COMPLETED.code),
                    sender
                )
            }
        }
    }
}
