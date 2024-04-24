package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util

class BlockPlaceActionType : BlockChangeActionType() {
    override val identifier = "block-place"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        world?.setBlockState(pos, oldBlockState())

        return true
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val state = newBlockState()
        world?.setBlockState(pos, state)
        if (state.hasBlockEntity()) {
            world?.getBlockEntity(pos)?.read(StringNbtReader.parse(extraData), server.registryManager)
        }

        return true
    }

    override fun getObjectMessage(source: ServerCommandSource): Text = Text.translatable(
        Util.createTranslationKey(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                objectIdentifier.toString().literal()
            )
        )
    }
}
