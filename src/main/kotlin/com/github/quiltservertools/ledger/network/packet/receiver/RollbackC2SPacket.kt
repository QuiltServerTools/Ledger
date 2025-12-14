package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import com.github.quiltservertools.ledger.network.packet.response.ResponseCodes
import com.github.quiltservertools.ledger.network.packet.response.ResponseContent
import com.github.quiltservertools.ledger.network.packet.response.ResponseS2CPacket
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class RollbackC2SPacket(val input: String) : CustomPacketPayload {

    override fun type() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<RollbackC2SPacket> {
        val ID: CustomPacketPayload.Type<RollbackC2SPacket> = CustomPacketPayload.Type(LedgerPacketTypes.ROLLBACK.id)
        val CODEC: StreamCodec<FriendlyByteBuf, RollbackC2SPacket> = CustomPacketPayload.codec({ _, _ -> TODO() }, {
            RollbackC2SPacket(it.readUtf())
        })

        override fun receive(payload: RollbackC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            val sender = context.responseSender()
            if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
                !Permissions.check(player, "ledger.commands.purge", CommandConsts.PERMISSION_LEVEL)
            ) {
                ResponseS2CPacket.sendResponse(
                    ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.NO_PERMISSION.code),
                    sender
                )
                return
            }

            val params = SearchParamArgument.get(payload.input, player.createCommandSourceStack())

            ResponseS2CPacket.sendResponse(
                ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.EXECUTING.code),
                sender
            )

            Ledger.launch {
                DatabaseManager.purgeActions(params)

                ResponseS2CPacket.sendResponse(
                    ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.COMPLETED.code),
                    sender
                )
            }
        }
    }
}
