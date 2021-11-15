package com.github.quiltservertools.ledger.actions

import net.minecraft.server.MinecraftServer

class ItemRemoveActionType : ItemChangeActionType() {
    override val identifier: String = "item-remove"

    override fun rollback(server: MinecraftServer): Boolean = addItem(server)

    override fun restore(server: MinecraftServer): Boolean = removeMatchingItem(server)
}
