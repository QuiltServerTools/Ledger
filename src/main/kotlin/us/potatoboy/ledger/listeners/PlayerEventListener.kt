package us.potatoboy.ledger.listeners

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
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionFactory
import us.potatoboy.ledger.callbacks.PlayerBlockPlaceCallback
import us.potatoboy.ledger.callbacks.PlayerInsertItemCallback
import us.potatoboy.ledger.callbacks.PlayerRemoveItemCallback
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.inspectBlock
import us.potatoboy.ledger.utility.isInspecting

fun registerPlayerListeners() {
    PlayerBlockBreakEvents.AFTER.register(::onBlockBreak)
    PlayerBlockPlaceCallback.EVENT.register(::onBlockPlace)
    ServerPlayConnectionEvents.JOIN.register(::onJoin)
    PlayerInsertItemCallback.EVENT.register(::onItemInsert)
    PlayerRemoveItemCallback.EVENT.register(::onItemRemove)
    AttackBlockCallback.EVENT.register(::onBlockAttack)
    UseBlockCallback.EVENT.register(::onUseBlock)
}

private fun onUseBlock(
    player: PlayerEntity,
    world: World,
    hand: Hand,
    blockHitResult: BlockHitResult
): ActionResult {
    if ((player as ServerPlayerEntity).isInspecting() && hand == Hand.MAIN_HAND) {
        player.inspectBlock(blockHitResult.blockPos.offset(blockHitResult.side))
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

    if ((player as ServerPlayerEntity).isInspecting()) {
        player.inspectBlock(pos)
        return ActionResult.SUCCESS
    }

    return ActionResult.PASS
}

private fun onItemRemove(itemStack: ItemStack, blockPos: BlockPos, player: ServerPlayerEntity) {
    DatabaseManager.logAction(
        ActionFactory.itemRemoveAction(
            player.serverWorld,
            itemStack,
            blockPos,
            player
        )
    )
}

private fun onItemInsert(itemStack: ItemStack, blockPos: BlockPos, player: ServerPlayerEntity) {
    DatabaseManager.logAction(
        ActionFactory.itemInsertAction(
            player.serverWorld,
            itemStack,
            blockPos,
            player
        )
    )
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
    context: ItemPlacementContext,
    blockEntity: BlockEntity?
) {
    DatabaseManager.logAction(
        ActionFactory.blockPlaceAction(
            world,
            pos,
            state,
            player as ServerPlayerEntity,
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
    DatabaseManager.logAction(
        ActionFactory.blockBreakAction(
            world,
            pos,
            state,
            player as ServerPlayerEntity,
            blockEntity
        )
    )
}
