package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.EntityDismountCallback
import com.github.quiltservertools.ledger.callbacks.EntityMountCallback
import com.github.quiltservertools.ledger.callbacks.ItemDropCallback
import com.github.quiltservertools.ledger.callbacks.ItemPickUpCallback
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
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

fun registerPlayerListeners() {
    PlayerBlockBreakEvents.AFTER.register(::onBlockBreak)
    ServerPlayConnectionEvents.JOIN.register(::onJoin)
    ServerPlayConnectionEvents.DISCONNECT.register(::onLeave)
    AttackBlockCallback.EVENT.register(::onBlockAttack)
    UseBlockCallback.EVENT.register(::onUseBlock)
    ItemPickUpCallback.EVENT.register(::onItemPickUp)
    ItemDropCallback.EVENT.register(::onItemDrop)
    EntityMountCallback.EVENT.register(::onEntityMount)
    EntityDismountCallback.EVENT.register(::onEntityDismount)
}

fun onLeave(handler: ServerGamePacketListenerImpl, server: MinecraftServer) {
    handler.player.disableNetworking()
}

private fun onUseBlock(
    player: Player,
    world: Level,
    hand: InteractionHand,
    blockHitResult: BlockHitResult
): InteractionResult {
    if (player is ServerPlayer && player.isInspecting() && hand == InteractionHand.MAIN_HAND) {
        player.createCommandSourceStack().inspectBlock(blockHitResult.blockPos.relative(blockHitResult.direction))
        return InteractionResult.SUCCESS
    }
    return InteractionResult.PASS
}

private fun onBlockAttack(
    player: Player,
    world: Level,
    hand: InteractionHand,
    pos: BlockPos,
    direction: Direction
): InteractionResult {
    if (player is ServerPlayer && player.isInspecting()) {
        player.createCommandSourceStack().inspectBlock(pos)
        return InteractionResult.SUCCESS
    }
    return InteractionResult.PASS
}

private fun onJoin(networkHandler: ServerGamePacketListenerImpl, packetSender: PacketSender, server: MinecraftServer) {
    Ledger.launch {
        DatabaseManager.logPlayer(networkHandler.player.uuid, networkHandler.player.scoreboardName)
    }
}

private fun onBlockPlace(
    world: Level,
    player: Player,
    pos: BlockPos,
    state: BlockState,
    context: BlockPlaceContext?,
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
    world: Level,
    player: Player,
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

private fun onItemPickUp(
    entity: ItemEntity,
    player: Player
) {
    ActionQueueService.addToQueue(ActionFactory.itemPickUpAction(entity, player))
}

private fun onItemDrop(
    entity: ItemEntity,
    playerOrGolem: LivingEntity
) {
    ActionQueueService.addToQueue(ActionFactory.itemDropAction(entity, playerOrGolem))
}

private fun onEntityMount(
    entity: Entity,
    playerEntity: Player,
) {
    ActionQueueService.addToQueue(ActionFactory.entityMountAction(entity, playerEntity))
}

private fun onEntityDismount(
    entity: Entity,
    playerEntity: Player,
) {
    ActionQueueService.addToQueue(ActionFactory.entityDismountAction(entity, playerEntity))
}
