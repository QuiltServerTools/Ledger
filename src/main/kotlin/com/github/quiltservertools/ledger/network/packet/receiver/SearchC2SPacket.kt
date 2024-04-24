package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import com.github.quiltservertools.ledger.network.packet.response.ResponseCodes
import com.github.quiltservertools.ledger.network.packet.response.ResponseContent
import com.github.quiltservertools.ledger.network.packet.response.ResponseS2CPacket
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.launchMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

data class SearchC2SPacket(val restore: Boolean, val args: String) : CustomPayload {

    override fun getId() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<SearchC2SPacket> {
        val ID: CustomPayload.Id<SearchC2SPacket> = CustomPayload.Id(LedgerPacketTypes.SEARCH.id)
        val CODEC: PacketCodec<PacketByteBuf, SearchC2SPacket> = CustomPayload.codecOf({ _, _ -> TODO() }, {
            SearchC2SPacket(it.readBoolean(), it.readString())
        })

        override fun receive(payload: SearchC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            val sender = context.responseSender()
            if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
                !Permissions.check(player, "ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL)
            ) {
                ResponseS2CPacket.sendResponse(
                    ResponseContent(
                        LedgerPacketTypes.ROLLBACK.id,
                        ResponseCodes.NO_PERMISSION.code
                    ),
                    sender
                )
                return
            }

            val source = player.commandSource

            val params = SearchParamArgument.get(payload.args, source)

            ResponseS2CPacket.sendResponse(
                ResponseContent(LedgerPacketTypes.ROLLBACK.id, ResponseCodes.EXECUTING.code),
                sender
            )

            Ledger.launch(Dispatchers.IO) {
                MessageUtils.warnBusy(source)
                if (payload.restore) {
                    val actions = DatabaseManager.restoreActions(params)

                    source.world.launchMain {
                        for (action in actions) {
                            action.restore(source.server)
                            action.rolledBack = false
                        }

                        ResponseS2CPacket.sendResponse(
                            ResponseContent(
                                LedgerPacketTypes.ROLLBACK.id,
                                ResponseCodes.COMPLETED.code
                            ),
                            sender
                        )
                    }
                } else {
                    val actions = DatabaseManager.rollbackActions(params)

                    source.world.launchMain {
                        for (action in actions) {
                            action.rollback(source.server)
                            action.rolledBack = true
                        }

                        ResponseS2CPacket.sendResponse(
                            ResponseContent(
                                LedgerPacketTypes.ROLLBACK.id,
                                ResponseCodes.COMPLETED.code
                            ),
                            sender
                        )
                    }
                }
            }
        }
    }
}
