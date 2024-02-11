package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

class ItemInsertActionType : ItemChangeActionType() {
    override val identifier: String = "item-insert"

    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) {
        previewItemChange(preview, player, false)
    }

    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) {
        previewItemChange(preview, player, true)
    }

    override fun rollback(server: MinecraftServer) = removeMatchingItem(server)

    override fun restore(server: MinecraftServer) = addItem(server)
}
