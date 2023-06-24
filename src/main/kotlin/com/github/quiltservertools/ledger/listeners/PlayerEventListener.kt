package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.database.ActionQueueService
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.network.Networking.disableNetworking
import com.github.quiltservertools.ledger.utility.inspectBlock
import com.github.quiltservertools.ledger.utility.isInspecting
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun registerPlayerListeners() {
    PlayerBlockBreakEvents.AFTER.register(::onBlockBreak)
    ServerPlayConnectionEvents.JOIN.register(::onJoin)
    ServerPlayConnectionEvents.DISCONNECT.register(::onLeave)
    AttackBlockCallback.EVENT.register(::onBlockAttack)
    UseBlockCallback.EVENT.register(::onUseBlock)
}

fun onLeave(handler: ServerPlayNetworkHandler, server: MinecraftServer) {
    handler.player.disableNetworking()
}

private fun onUseBlock(
    player: PlayerEntity,
    world: World,
    hand: Hand,
    blockHitResult: BlockHitResult
): ActionResult {
    if (player.isInspecting() && hand == Hand.MAIN_HAND) {
        player.commandSource.inspectBlock(blockHitResult.blockPos.offset(blockHitResult.side))
        return ActionResult.SUCCESS
    }

    return ActionResult.PASS
}

private fun onBlockAttack(
    player: PlayerEntity,
    world: World,
    hand: Hand,
    pos: BlockPos,
    direction: Direction
): ActionResult {
    if (world.isClient) return ActionResult.PASS

    if (player.isInspecting()) {
        player.commandSource.inspectBlock(pos)
        return ActionResult.SUCCESS
    }

    return ActionResult.PASS
}


private fun onJoin(networkHandler: ServerPlayNetworkHandler, packetSender: PacketSender, server: MinecraftServer) {
    Ledger.launch {
        DatabaseManager.logPlayer(networkHandler.player.uuid, networkHandler.player.entityName)
    }
}

private fun onBlockPlace(
    world: World,
    player: PlayerEntity,
    pos: BlockPos,
    state: BlockState,
    context: ItemPlacementContext?,
    blockEntity: BlockEntity?
) {
    ActionQueueService.addToQueue(
        ActionFactory.blockPlaceAction(
            world,
            pos,
            state,
            player,
            blockEntity
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
    ActionQueueService.addToQueue(
        ActionFactory.blockBreakAction(
            world,
            pos,
            state,
            player,
            blockEntity
        )
    )
}
