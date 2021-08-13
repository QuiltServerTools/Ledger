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
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity

class PurgeReceiver : Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
            !Permissions.check(player, "ledger.commands.purge", CommandConsts.PERMISSION_LEVEL)) {
            ResponsePacket.sendResponse(ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.NO_PERMISSION.code), sender)
            return
        }

        val params = SearchParamArgument.get(buf.readString(), player.commandSource)

        ResponsePacket.sendResponse(ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.EXECUTING.code), sender)

        Ledger.launch {

            DatabaseManager.purgeActions(params)

            ResponsePacket.sendResponse(ResponseContent(LedgerPacketTypes.PURGE.id, ResponseCodes.COMPLETED.code), sender)
        }
    }
}
