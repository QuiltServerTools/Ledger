package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.logWarn
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.nbt.StringNbtReader
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util

open class BlockChangeActionType : AbstractActionType() {
    override val identifier = "block-change"

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)
        world?.setBlockState(pos, oldBlockState())
        world?.getBlockEntity(pos)?.read(StringNbtReader.parse(extraData), server.registryManager)
        world?.chunkManager?.markForUpdate(pos)

        return true
    }

    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) {
        if (player.world.registryKey.value == world) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, oldBlockState()))
            preview.positions.add(pos)
        }
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        world?.setBlockState(pos, newBlockState())

        return true
    }

    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) {
        if (player.world.registryKey.value == world) {
            player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, newBlockState()))
            preview.positions.add(pos)
        }
    }

    override fun getTranslationType() = "block"

    override fun getObjectMessage(source: ServerCommandSource): Text {
        val text = Text.literal("")
        text.append(
            Text.translatable(
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
        )
        if (oldObjectIdentifier != objectIdentifier) {
            text.append(" → ".literal())
            text.append(
                Text.translatable(
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
            )
        }
        return text
    }

    fun oldBlockState() = checkForBlockState(
        oldObjectIdentifier,
        oldObjectState?.let {
        NbtUtils.blockStateFromProperties(
            StringNbtReader.parse(it),
            oldObjectIdentifier
        )
    }
    )

    fun newBlockState() = checkForBlockState(
        objectIdentifier,
        objectState?.let {
        NbtUtils.blockStateFromProperties(
            StringNbtReader.parse(it),
            objectIdentifier
        )
    }
    )

    private fun checkForBlockState(identifier: Identifier, checkState: BlockState?): BlockState {
        val block = Registries.BLOCK.getOrEmpty(identifier)
        if (block.isEmpty) {
            logWarn("Unknown block $identifier")
            return Blocks.AIR.defaultState
        }

        var state = block.get().defaultState
        if (checkState != null) state = checkState

        return state
    }
}
