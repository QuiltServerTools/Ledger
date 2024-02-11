package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

class ItemRemoveActionType : ItemChangeActionType() {
    override val identifier: String = "item-remove"

    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) {
        previewItemChange(preview, player, true)
    }

    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) {
        previewItemChange(preview, player, false)
    }

    override fun rollback(server: MinecraftServer): Boolean = addItem(server)

    override fun restore(server: MinecraftServer): Boolean = removeMatchingItem(server)
}
