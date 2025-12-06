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
import com.github.quiltservertools.ledger.utility.TextColorPallet
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class SearchC2SPacket(val args: String, val pages: Int) : CustomPacketPayload {

    override fun type() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<SearchC2SPacket> {
        val ID: CustomPacketPayload.Type<SearchC2SPacket> = CustomPacketPayload.Type(LedgerPacketTypes.SEARCH.id)
        val CODEC: StreamCodec<FriendlyByteBuf, SearchC2SPacket> = CustomPacketPayload.codec({ _, _ -> TODO() }, {
            SearchC2SPacket(it.readUtf(), it.readInt())
        })

        override fun receive(payload: SearchC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            val sender = context.responseSender()
            if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
                !Permissions.check(player, "ledger.commands.search", CommandConsts.PERMISSION_LEVEL)
            ) {
                ResponseS2CPacket.sendResponse(
                    ResponseContent(
                        LedgerPacketTypes.SEARCH.id,
                        ResponseCodes.NO_PERMISSION.code
                    ),
                    sender
                )
                return
            }

            val source = player.createCommandSourceStack()

            val params = SearchParamArgument.get(payload.args, source)

            ResponseS2CPacket.sendResponse(
                ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.EXECUTING.code),
                sender
            )

            Ledger.launch {
                Ledger.searchCache[source.textName] = params

                MessageUtils.warnBusy(source)
                val results = DatabaseManager.searchActions(params, 1)

                for (i in 1..payload.pages) {
                    val page = DatabaseManager.searchActions(results.searchParams, i)
                    MessageUtils.sendSearchResults(
                        source,
                        page,
                        Component.translatable(
                            "text.ledger.header.search"
                        ).setStyle(TextColorPallet.primary)
                    )
                }

                ResponseS2CPacket.sendResponse(
                    ResponseContent(
                        LedgerPacketTypes.SEARCH.id,
                        ResponseCodes.COMPLETED.code
                    ),
                    sender
                )
            }
        }
    }
}
