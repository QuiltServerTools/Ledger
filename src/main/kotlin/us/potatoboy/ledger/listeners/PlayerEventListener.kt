package us.potatoboy.ledger.listeners

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtHelper
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import us.potatoboy.ledger.actions.ActionFactory
import us.potatoboy.ledger.actions.BlockBreakActionType
import us.potatoboy.ledger.actions.BlockPlaceActionType
import us.potatoboy.ledger.callbacks.PlayerBlockPlaceCallback
import us.potatoboy.ledger.database.ActionQueue
import us.potatoboy.ledger.database.DatabaseManager
import java.time.Instant

object PlayerEventListener {
    init {
        PlayerBlockBreakEvents.AFTER.register(::onBlockBreak)
        PlayerBlockPlaceCallback.EVENT.register(::onBlockPlace)
        ServerPlayConnectionEvents.JOIN.register(::onJoin)
    }

    private fun onJoin(networkHandler: ServerPlayNetworkHandler, packetSender: PacketSender, server: MinecraftServer) {
        GlobalScope.launch(Dispatchers.IO) {
            DatabaseManager.addPlayer(networkHandler.player.uuid, networkHandler.player.entityName)
        }
    }

    private fun onBlockPlace(world: World, player: PlayerEntity, pos: BlockPos, state: BlockState, entity: BlockEntity?, context: ItemPlacementContext) {
        ActionQueue.addActionToQueue(ActionFactory.blockPlaceAction(world, pos, state, player as ServerPlayerEntity))
    }

    private fun onBlockBreak(world: World, player: PlayerEntity, pos: BlockPos, state: BlockState, blockEntity: BlockEntity?) {
        ActionQueue.addActionToQueue(ActionFactory.blockBreakAction(world, pos, state, player as ServerPlayerEntity))
    }
}