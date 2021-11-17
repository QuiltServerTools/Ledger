package com.github.quiltservertools.ledger.actions

import net.minecraft.server.MinecraftServer

class EntityItemRemoveActionType : EntityItemChangeActionType() {
    override val identifier: String = "entity-item-r"

    override fun rollback(server: MinecraftServer): Boolean = add(server)

    override fun restore(server: MinecraftServer): Boolean = remove(server)
}
