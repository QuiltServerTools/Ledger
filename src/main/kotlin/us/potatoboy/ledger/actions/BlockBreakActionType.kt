package us.potatoboy.ledger.actions

import net.minecraft.block.Block
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.Registry

class BlockBreakActionType() : AbstractActionType() {
    override val identifier: String = "block-break"
    override fun getTranslationType(): String = "block"

    override fun rollback(world: ServerWorld): Boolean {
        val block = Registry.BLOCK.getOrEmpty(objectIdentifier)
        if (block.isEmpty) return false

        var state = block.get().defaultState
        if (this.blockState != null) state = this.blockState

        world.setBlockState(pos, state)

        return true
    }
}