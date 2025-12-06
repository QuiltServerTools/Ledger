package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.utility.LOGGER
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.Util
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.HolderGetter
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TagParser
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.storage.TagValueInput

open class BlockChangeActionType : AbstractActionType() {
    override val identifier = "block-change"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        world?.setBlockAndUpdate(pos, oldBlockState(world.holderLookup(Registries.BLOCK)))
        ProblemReporter.ScopedCollector({ "ledger:rollback:block-change@$pos" }, LOGGER).use {
            world?.getBlockEntity(pos)?.loadWithComponents(
                TagValueInput.create(
                    it,
                    server.registryAccess(),
                    TagParser.parseCompoundFully(extraData)
                )
            )
        }
        world?.chunkSource?.blockChanged(pos)

        return true
    }

    override fun previewRollback(preview: Preview, player: ServerPlayer) {
        if (player.level().dimension().location() == world) {
            player.connection.send(
                ClientboundBlockUpdatePacket(
                    pos,
                    oldBlockState(player.level().holderLookup(Registries.BLOCK))
                )
            )
            preview.positions.add(pos)
        }
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        world?.setBlockAndUpdate(pos, newBlockState(world.holderLookup(Registries.BLOCK)))

        return true
    }

    override fun previewRestore(preview: Preview, player: ServerPlayer) {
        if (player.level().dimension().location() == world) {
            player.connection.send(
                ClientboundBlockUpdatePacket(
                    pos,
                    newBlockState(player.level().holderLookup(Registries.BLOCK))
                )
            )
            preview.positions.add(pos)
        }
    }

    override fun getTranslationType() = "block"

    override fun getObjectMessage(source: CommandSourceStack): Component {
        val text = Component.literal("")
        text.append(
            Component.translatable(
            Util.makeDescriptionId(
                this.getTranslationType(),
                oldObjectIdentifier
            )
        ).setStyle(TextColorPallet.secondaryVariant).withStyle {
            it.withHoverEvent(
                HoverEvent.ShowText(
                    oldObjectIdentifier.toString().literal()
                )
            )
        }
        )
        if (oldObjectIdentifier != objectIdentifier) {
            text.append(" → ".literal())
            text.append(
                Component.translatable(
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
            )
        }
        return text
    }

    fun oldBlockState(blockLookup: HolderGetter<Block>) = checkForBlockState(
        oldObjectIdentifier,
        oldObjectState?.let {
            NbtUtils.blockStateFromProperties(
                TagParser.parseCompoundFully(it),
                oldObjectIdentifier,
                blockLookup
            )
        }
    )

    fun newBlockState(blockLookup: HolderGetter<Block>) = checkForBlockState(
        objectIdentifier,
        objectState?.let {
            NbtUtils.blockStateFromProperties(
                TagParser.parseCompoundFully(it),
                objectIdentifier,
                blockLookup
            )
        }
    )

    private fun checkForBlockState(identifier: ResourceLocation, checkState: BlockState?): BlockState {
        val block = BuiltInRegistries.BLOCK.getOptional(identifier)
        if (block.isEmpty) {
            logWarn("Unknown block $identifier")
            return Blocks.AIR.defaultBlockState()
        }

        var state = block.get().defaultBlockState()
        if (checkState != null) state = checkState

        return state
    }
}
