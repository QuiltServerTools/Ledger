package com.github.quiltservertools.ledger.actions

import net.minecraft.server.MinecraftServer

class ItemInsertActionType : ItemChangeActionType() {
    override val identifier: String = "item-insert"

    override fun rollback(server: MinecraftServer) = removeMatchingItem(server)

    override fun restore(server: MinecraftServer) = addItem(server)
}
