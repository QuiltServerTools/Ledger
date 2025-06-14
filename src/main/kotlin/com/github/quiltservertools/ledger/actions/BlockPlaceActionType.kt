package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.LOGGER
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.storage.NbtReadView
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.ErrorReporter
import net.minecraft.util.Util

class BlockPlaceActionType : BlockChangeActionType() {
    override val identifier = "block-place"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        world?.setBlockState(pos, oldBlockState(world.createCommandRegistryWrapper(RegistryKeys.BLOCK)))

        return world != null
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        if (world != null) {
            val state = newBlockState(world.createCommandRegistryWrapper(RegistryKeys.BLOCK))
            world.setBlockState(pos, state)
            if (state.hasBlockEntity()) {
                ErrorReporter.Logging({ "ledger:restore:block-place@$pos" }, LOGGER).use {
                    world.getBlockEntity(pos)?.read(
                        NbtReadView.create(
                            it,
                            server.registryManager,
                            StringNbtReader.readCompound(extraData)
                        )
                    )
                }
            }
        }

        return world != null
    }

    override fun getObjectMessage(source: ServerCommandSource): Text = Text.translatable(
        Util.createTranslationKey(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).styled {
        it.withHoverEvent(
            HoverEvent.ShowText(
                objectIdentifier.toString().literal()
            )
        )
    }
}
