package com.github.quiltservertools.ledger.listeners

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import com.github.quiltservertools.ledger.callbacks.ItemFrameInsertCallback
import com.github.quiltservertools.ledger.callbacks.ItemFrameRemoveCallback
import com.github.quiltservertools.ledger.callbacks.ItemInsertCallback
import com.github.quiltservertools.ledger.callbacks.ItemRemoveCallback
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.itemInsertAction
import com.github.quiltservertools.ledger.utility.itemRemoveAction
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos

fun registerWorldEventListeners() {
    ItemInsertCallback.EVENT.register(::onItemInsert)
    ItemRemoveCallback.EVENT.register(::onItemRemove)
    ServerWorldEvents.LOAD.register(::onWorldLoad)
    ItemFrameInsertCallback.EVENT.register(::onItemInsert)
    ItemFrameRemoveCallback.EVENT.register(::onItemRemove)
}

fun onWorldLoad(server: MinecraftServer, world: ServerWorld) {
    Ledger.launch {
        DatabaseManager.registerWorld(world.registryKey.value)
    }
}

private fun onItemRemove(
    stack: ItemStack,
    pos: BlockPos,
    world: ServerWorld,
    source: String,
    player: ServerPlayerEntity?
) {
    if (player != null) {
        DatabaseManager.logAction(
            ActionFactory.itemRemoveAction(world, stack, pos, player)
        )
    } else {
        DatabaseManager.logAction(
            ActionFactory.itemRemoveAction(world, stack, pos, source)
        )
    }
}

private fun onItemInsert(
    stack: ItemStack,
    pos: BlockPos,
    world: ServerWorld,
    source: String,
    player: ServerPlayerEntity?
) {
    if (player != null) {
        DatabaseManager.logAction(
            ActionFactory.itemInsertAction(world, stack, pos, player)
        )
    } else {
        DatabaseManager.logAction(
            ActionFactory.itemInsertAction(world, stack, pos, source)
        )
    }
}

private fun onItemInsert(
    stack: ItemStack,
    pos: BlockPos,
    world: ServerWorld,
    source: String,
    player: ServerPlayerEntity?,
    itemFrameEntity: ItemFrameEntity
) {
    DatabaseManager.logAction(
        ActionFactory.itemInsertAction(world, stack, pos, source, player, itemFrameEntity)
    )

}

private fun onItemRemove(
    stack: ItemStack,
    pos: BlockPos,
    world: ServerWorld,
    source: String,
    player: ServerPlayerEntity?,
    itemFrameEntity: ItemFrameEntity
) {
    DatabaseManager.logAction(
        ActionFactory.itemRemoveAction(world, stack, pos, source, player, itemFrameEntity)
    )
}
