package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.LOGGER
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ProblemReporter
import net.minecraft.util.Util
import net.minecraft.world.level.storage.TagValueInput

class BlockPlaceActionType : BlockChangeActionType() {
    override val identifier = "block-place"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        world?.setBlockAndUpdate(pos, oldBlockState(world.holderLookup(Registries.BLOCK)))

        return world != null
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        if (world != null) {
            val state = newBlockState(world.holderLookup(Registries.BLOCK))
            world.setBlockAndUpdate(pos, state)
            if (state.hasBlockEntity()) {
                ProblemReporter.ScopedCollector({ "ledger:restore:block-place@$pos" }, LOGGER).use {
                    world.getBlockEntity(pos)?.loadWithComponents(
                        TagValueInput.create(
                            it,
                            server.registryAccess(),
                            TagParser.parseCompoundFully(extraData!!)
                        )
                    )
                }
            }
        }

        return world != null
    }

    override fun getObjectMessage(source: CommandSourceStack): Component = Component.translatable(
        Util.makeDescriptionId(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).withStyle {
        it.withHoverEvent(
            HoverEvent.ShowText(
                objectIdentifier.toString().literal()
            )
        )
    }
}
