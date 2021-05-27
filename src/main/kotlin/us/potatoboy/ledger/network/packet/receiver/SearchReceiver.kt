package us.potatoboy.ledger.network.packet.receiver

import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.commands.arguments.SearchParamArgument
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.network.packet.LedgerPacketTypes
import us.potatoboy.ledger.network.packet.Receiver
import us.potatoboy.ledger.network.packet.action.ActionPacket

class SearchReceiver: Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        val source = player.commandSource
        val input = buf.readString()
        val params = SearchParamArgument.get(input, source)

        if (params.isEmpty()) {
            source.sendError(TranslatableText("error.ledger.command.no_params"))
            return
        }

        Ledger.launch {
            Ledger.searchCache[source.name] = params

            val results = DatabaseManager.searchActions(params, 1, source)

            if (results.actions.isEmpty()) {
                source.sendError(TranslatableText("error.ledger.command.no_results"))
                return@launch
            }
            results.actions.forEach { action: ActionType -> run {
                val packet = ActionPacket()
                packet.populate(action)
                sender.sendPacket(LedgerPacketTypes.ACTION.id, packet.buf)
            } }
        }
    }
}
