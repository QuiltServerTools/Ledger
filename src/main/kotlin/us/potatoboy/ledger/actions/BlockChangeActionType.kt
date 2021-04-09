package us.potatoboy.ledger.actions

import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.Registry

open class BlockChangeActionType(override val identifier: String) : AbstractActionType() {
    override fun rollback(world: ServerWorld): Boolean {
        val oldBlock = Registry.BLOCK.getOrEmpty(oldObjectIdentifier)
        if (oldBlock.isEmpty) return false

        var state = oldBlock.get().defaultState
        if (this.blockState != null) state = this.oldBlockState

        world.setBlockState(pos, state)

        return true
    }

    override fun preview(world: ServerWorld, player: ServerPlayerEntity) {
        val block = Registry.BLOCK.getOrEmpty(oldObjectIdentifier)
        if (block.isEmpty) return

        var state = block.get().defaultState
        if (this.blockState != null) state = this.oldBlockState

        player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, state))
    }

    override fun restore(world: ServerWorld): Boolean {
        val block = Registry.BLOCK.getOrEmpty(objectIdentifier)
        if (block.isEmpty) return false

        var state = block.get().defaultState
        if (this.blockState != null) state = this.blockState

        world.setBlockState(pos, state)

        return true
    }

    override fun getTranslationType() = "block"
}