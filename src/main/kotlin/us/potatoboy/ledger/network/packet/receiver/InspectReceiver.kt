package us.potatoboy.ledger.network.packet.receiver

import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.getInspectResults
import us.potatoboy.ledger.network.packet.Receiver
import us.potatoboy.ledger.network.packet.action.ActionPacket

class InspectReceiver : Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        val pos = buf.readBlockPos()
        Ledger.launch {
            val results = player.getInspectResults(pos)
            results.actions.forEach { action ->
                run {
                    val packet = ActionPacket()
                    packet.populate(action)
                    sender.sendPacket(packet.channel, packet.buf)
                }
            }
        }

    }
}
