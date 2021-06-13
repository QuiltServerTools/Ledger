package com.github.quiltservertools.ledger.actions

import net.minecraft.server.world.ServerWorld

class ItemInsertActionType : ItemChangeActionType() {
    override val identifier: String = "item-insert"

    override fun rollback(world: ServerWorld) = getInventory(world)?.let { removeMatchingItem(it) } ?: false

    override fun restore(world: ServerWorld) = getInventory(world)?.let { addItem(it) } ?: false
}
