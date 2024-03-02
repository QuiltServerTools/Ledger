package com.github.quiltservertools.ledger.testmod

import com.github.quiltservertools.ledger.testmod.commands.packet.ActionS2CPacket
import com.github.quiltservertools.ledger.testmod.commands.packet.HandshakeC2SPacket
import com.github.quiltservertools.ledger.testmod.commands.packet.HandshakeS2CPacket
import com.github.quiltservertools.ledger.testmod.commands.packet.InspectC2SPacket
import com.github.quiltservertools.ledger.testmod.commands.packet.SearchC2SPacket
import com.github.quiltservertools.ledger.testmod.commands.registerCommands
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object LedgerTest : ClientModInitializer {
    val HANDSHAKE = Identifier("ledger", "handshake")
    val INSPECT = Identifier("ledger", "inspect")
    val SEARCH = Identifier("ledger", "search")
    val ACTION = Identifier("ledger", "action")
    val LOGGER: Logger = LogManager.getLogger("LedgerTestmod")

    override fun onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(HandshakeC2SPacket.ID, HandshakeC2SPacket.CODEC)
        PayloadTypeRegistry.playC2S().register(SearchC2SPacket.ID, SearchC2SPacket.CODEC)
        PayloadTypeRegistry.playC2S().register(InspectC2SPacket.ID, InspectC2SPacket.CODEC)

        PayloadTypeRegistry.playS2C().register(HandshakeS2CPacket.ID, HandshakeS2CPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ActionS2CPacket.ID, ActionS2CPacket.CODEC)

        ClientPlayNetworking.registerGlobalReceiver(HandshakeS2CPacket.ID, HandshakeS2CPacket)
        ClientPlayNetworking.registerGlobalReceiver(ActionS2CPacket.ID, ActionS2CPacket)

        PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, blockEntity ->
            inspectBlock(pos)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            registerCommands(dispatcher)
        }
    }

    fun inspectBlock(pos: BlockPos) {
        ClientPlayNetworking.send(InspectC2SPacket(pos))
    }

    fun sendSearchQuery(query: String) {
        ClientPlayNetworking.send(SearchC2SPacket(query))
    }
}
