package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import net.minecraft.util.registry.Registry

class BlockInteractActionType : BlockChangeActionType("block-interact") {
    override fun getObjectMessage(): Text = TranslatableText(
            Util.createTranslationKey(
                this.getTranslationType(),
                oldObjectIdentifier
            )
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    oldObjectIdentifier.toString().literal()
                )
            )
        }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val success = super.rollback(server)
        val oldBlock = Registry.BLOCK.get(oldObjectIdentifier)

        var state = oldBlock.defaultState
        if (this.oldBlockState != null) state = this.oldBlockState

        world?.setBlockState(pos, state)

        if (success && world?.getBlockEntity(pos) != null) {
            world.getBlockEntity(pos)?.readNbt(StringNbtReader.parse(extraData))
        }

        return success
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val block = Registry.BLOCK.get(objectIdentifier)

        var state = block.defaultState
        if (this.blockState != null) state = this.blockState

        world?.setBlockState(pos, state)

        return true
    }
}
