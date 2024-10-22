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
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.text.Text

data class SearchC2SPacket(val args: String, val pages: Int) : CustomPayload {

    override fun getId() = ID

    companion object : ServerPlayNetworking.PlayPayloadHandler<SearchC2SPacket> {
        val ID: CustomPayload.Id<SearchC2SPacket> = CustomPayload.Id(LedgerPacketTypes.SEARCH.id)
        val CODEC: PacketCodec<PacketByteBuf, SearchC2SPacket> = CustomPayload.codecOf({ _, _ -> TODO() }, {
            SearchC2SPacket(it.readString(), it.readInt())
        })

        override fun receive(payload: SearchC2SPacket, context: ServerPlayNetworking.Context) {
            val player = context.player()
            val sender = context.responseSender()
            if (!Permissions.check(player.commandSource, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
                !Permissions.check(player.commandSource, "ledger.commands.search", CommandConsts.PERMISSION_LEVEL)
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

            val source = player.commandSource

            val params = SearchParamArgument.get(payload.args, source)

            ResponseS2CPacket.sendResponse(
                ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.EXECUTING.code),
                sender
            )

            Ledger.launch {
                Ledger.searchCache[source.name] = params

                MessageUtils.warnBusy(source)
                val results = DatabaseManager.searchActions(params, 1)

                for (i in 1..payload.pages) {
                    val page = DatabaseManager.searchActions(results.searchParams, i)
                    MessageUtils.sendSearchResults(
                        source,
                        page,
                        Text.translatable(
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
