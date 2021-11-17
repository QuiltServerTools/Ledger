package com.github.quiltservertools.ledger.actions

import net.minecraft.server.MinecraftServer

class EntityItemInsertActionType : EntityItemChangeActionType() {
    override val identifier: String = "entity-item-a"

    override fun rollback(server: MinecraftServer) = remove(server)

    override fun restore(server: MinecraftServer) = add(server)
}
