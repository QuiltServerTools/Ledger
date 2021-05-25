package us.potatoboy.ledger.listeners

import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import us.potatoboy.ledger.actionutils.ActionFactory
import us.potatoboy.ledger.callbacks.PlayerBlockPlaceCallback
import us.potatoboy.ledger.callbacks.PlayerInsertItemCallback
import us.potatoboy.ledger.callbacks.PlayerRemoveItemCallback
import us.potatoboy.ledger.database.DatabaseQueue
import us.potatoboy.ledger.database.queueitems.ActionQueueItem
import us.potatoboy.ledger.database.queueitems.PlayerQueueItem
import us.potatoboy.ledger.inspectBlock
import us.potatoboy.ledger.isInspecting
import us.potatoboy.ledger.network.packet.action.ActionPacket

object PlayerEventListener {
    init {
        PlayerBlockBreakEvents.AFTER.register(::onBlockBreak)
        PlayerBlockPlaceCallback.EVENT.register(::onBlockPlace)
        ServerPlayConnectionEvents.JOIN.register(::onJoin)
        PlayerInsertItemCallback.EVENT.register(::onItemInsert)
        PlayerRemoveItemCallback.EVENT.register(::onItemRemove)
        AttackBlockCallback.EVENT.register(::onBlockAttack)
    }

    private fun onBlockAttack(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        pos: BlockPos,
        direction: Direction
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS

        if ((player as ServerPlayerEntity).isInspecting()) {
            player.inspectBlock(pos)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    private fun onItemRemove(itemStack: ItemStack, blockPos: BlockPos, player: ServerPlayerEntity) {
        DatabaseQueue.addActionToQueue(
            ActionQueueItem(
                ActionFactory.itemRemoveAction(
                    player.serverWorld,
                    itemStack,
                    blockPos,
                    player
                )
            )
        )
    }

    private fun onItemInsert(itemStack: ItemStack, blockPos: BlockPos, player: ServerPlayerEntity) {
        DatabaseQueue.addActionToQueue(
            ActionQueueItem(
                ActionFactory.itemInsertAction(
                    player.serverWorld,
                    itemStack,
                    blockPos,
                    player
                )
            )
        )
    }

    private fun onJoin(networkHandler: ServerPlayNetworkHandler, packetSender: PacketSender, server: MinecraftServer) {
        DatabaseQueue.addActionToQueue(PlayerQueueItem(networkHandler.player.uuid, networkHandler.player.entityName))
        val p = ActionPacket()
        ServerPlayNetworking.send(networkHandler.player, p.channel, p.buf)
    }

    private fun onBlockPlace(
        world: World,
        player: PlayerEntity,
        pos: BlockPos,
        state: BlockState,
        context: ItemPlacementContext,
        blockEntity: BlockEntity?
    ) {
        DatabaseQueue.addActionToQueue(
            ActionQueueItem(
                ActionFactory.blockPlaceAction(
                    world,
                    pos,
                    state,
                    player as ServerPlayerEntity,
                    blockEntity
                )
            )
        )
    }

    private fun onBlockBreak(
        world: World,
        player: PlayerEntity,
        pos: BlockPos,
        state: BlockState,
        blockEntity: BlockEntity?
    ) {
        DatabaseQueue.addActionToQueue(
            ActionQueueItem(
                ActionFactory.blockBreakAction(
                    world,
                    pos,
                    state,
                    player as ServerPlayerEntity,
                    blockEntity
                )
            )
        )
    }
}
