package com.github.quiltservertools.ledger.network.packet.receiver

import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.PacketByteBuf
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.network.packet.Receiver
import com.github.quiltservertools.ledger.network.packet.action.ActionPacket
import com.github.quiltservertools.ledger.utility.getInspectResults

class InspectReceiver : Receiver {
    override fun receive(
        server: MinecraftServer,
        player: ServerPlayerEntity,
        handler: ServerPlayNetworkHandler,
        buf: PacketByteBuf,
        sender: PacketSender
    ) {
        if (!Permissions.check(player, "ledger.networking", CommandConsts.PERMISSION_LEVEL) ||
            !Permissions.check(player, "ledger.commands.inspect", CommandConsts.PERMISSION_LEVEL)
        ) return
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
