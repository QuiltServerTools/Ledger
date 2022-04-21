package com.github.quiltservertools.ledger.network.packet.receiver

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.commands.arguments.SearchParamArgument
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.packet.LedgerPacketTypes
import com.github.quiltservertools.ledger.network.packet.Receiver
import com.github.quiltservertools.ledger.network.packet.response.ResponseCodes
import com.github.quiltservertools.ledger.network.packet.response.ResponseContent
import com.github.quiltservertools.ledger.network.packet.response.ResponsePacket
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

class SearchReceiver : Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
            !Permissions.check(player, "ledger.commands.search", CommandConsts.PERMISSION_LEVEL)
        ) {
            ResponsePacket.sendResponse(
                ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.NO_PERMISSION.code),
                sender
            )
            return
        }
        val source = player.commandSource
        val input = buf.readString()
        val params = SearchParamArgument.get(input, source)

        val pages = buf.readInt()

        if (params.isEmpty()) {
            ResponsePacket.sendResponse(
                ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.ERROR.code),
                sender
            )
            return
        }

        ResponsePacket.sendResponse(
            ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.EXECUTING.code),
            sender
        )

        Ledger.launch {
            Ledger.searchCache[source.name] = params

            MessageUtils.warnBusy(source)
            val results = DatabaseManager.searchActions(params, 1)

            for (i in 1..pages) {
                val page = DatabaseManager.searchActions(results.searchParams, i)
                MessageUtils.sendSearchResults(
                    source,
                    page,
                    Text.translatable(
                        "text.ledger.header.search"
                    ).setStyle(TextColorPallet.primary)
                )
            }

            ResponsePacket.sendResponse(
                ResponseContent(LedgerPacketTypes.SEARCH.id, ResponseCodes.COMPLETED.code),
                sender
            )
        }
    }
}
