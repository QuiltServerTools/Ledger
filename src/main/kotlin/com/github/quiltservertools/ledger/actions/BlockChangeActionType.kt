package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.logWarn
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.nbt.StringNbtReader
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

open class BlockChangeActionType(override val identifier: String) : AbstractActionType() {
    override fun rollback(world: ServerWorld): Boolean {
        world.setBlockState(pos, oldBlockState())

        return true
    }

    override fun previewRollback(world: ServerWorld, player: ServerPlayerEntity) {
        player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, oldBlockState()))
    }

    override fun restore(world: ServerWorld): Boolean {
        world.setBlockState(pos, newBlockState())
        if (newBlockState().hasBlockEntity()) {
            world.getBlockEntity(pos)?.readNbt(StringNbtReader.parse(extraData))
        }

        return true
    }

    override fun previewRestore(world: ServerWorld, player: ServerPlayerEntity) {
        player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, newBlockState()))
    }

    override fun getTranslationType() = "block"

    private fun oldBlockState() = checkForBlockState(oldObjectIdentifier, oldBlockState)

    private fun newBlockState() = checkForBlockState(objectIdentifier, blockState)

    private fun checkForBlockState(identifier: Identifier, checkState: BlockState?): BlockState {
        val block = Registry.BLOCK.getOrEmpty(identifier)
        if (block.isEmpty) {
            logWarn("Unknown block $identifier")
            return Blocks.AIR.defaultState
        }

        var state = block.get().defaultState
        if (checkState != null) state = checkState

        return state
    }
}
