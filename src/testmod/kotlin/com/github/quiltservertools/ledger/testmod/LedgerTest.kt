package com.github.quiltservertools.ledger.testmod

import com.github.quiltservertools.ledger.testmod.commands.registerCommands
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Instant

object LedgerTest : ClientModInitializer {
    val HANDSHAKE = Identifier("ledger", "handshake")
    val INSPECT = Identifier("ledger", "inspect")
    val SEARCH = Identifier("ledger", "search")
    val ACTION = Identifier("ledger", "action")
    val LOGGER: Logger = LogManager.getLogger("LedgerTestmod")

    override fun onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register { handler, sender, client ->
            // Create and send handshake packet
            val tag = NbtCompound()
            tag.putString("modid", "ledger-testmod")
            tag.putString("version", "1.0.0")
            tag.putInt("protocol_version", 1)
            val buf = PacketByteBufs.create()
            buf.writeNbt(tag)
            sender.sendPacket(HANDSHAKE, buf)
        }

        ClientPlayNetworking.registerGlobalReceiver(HANDSHAKE) { client, handler, buf, sender ->
            val protocolVersion = buf.readInt()
            val ledgerVersion = buf.readString()
            val actionsLength = buf.readInt()
            LOGGER.info("Protocol version: {}", protocolVersion)
            LOGGER.info("Ledger version: {}", ledgerVersion)
            LOGGER.info("Number of types registered: {}", actionsLength)
            for (i in 0..actionsLength) {
                LOGGER.info("Action type: {}", buf.readString())
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(ACTION) { client, handler, buf, sender ->
            val pos = buf.readBlockPos()
            val id = buf.readString()
            val world = buf.readIdentifier()
            val oldObjectId = buf.readIdentifier()
            val objectId = buf.readIdentifier()
            val source = buf.readString()
            val timestamp = Instant.ofEpochSecond(buf.readLong())
            val extraData = buf.readString()

            LOGGER.info("pos={}, id={}, world={}, oldObjectId={}, objectId={}, source={}, timestamp={}, extraData={}",
                pos, id, world, oldObjectId, objectId, source, timestamp, extraData)
        }

        PlayerBlockBreakEvents.AFTER.register { world, player, pos, state, blockEntity ->
            inspectBlock(pos)
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            registerCommands(dispatcher)
        }
    }

    fun inspectBlock(pos: BlockPos) {
        val buf = PacketByteBufs.create()
        buf.writeBlockPos(pos)
        ClientPlayNetworking.send(INSPECT, buf)
    }

    fun sendSearchQuery(query: String) {
        val buf = PacketByteBufs.create()
        buf.writeString(query)
        ClientPlayNetworking.send(SEARCH, buf)
    }
}
