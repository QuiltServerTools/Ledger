package us.potatoboy.ledger.actions

import net.minecraft.server.world.ServerWorld

class ItemInsertActionType : ItemChangeActionType() {
    override val identifier: String = "item-insert"

    override fun rollback(world: ServerWorld): Boolean {
        return getInventory(world)?.let { removeMatchingItem(it) } ?: false
    }

    override fun restore(world: ServerWorld): Boolean {
        return getInventory(world)?.let { addItem(it) } ?: false
    }
}