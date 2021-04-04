package us.potatoboy.ledger.actions

import net.minecraft.block.Blocks
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.registry.Registry

class BlockPlaceActionType() : AbstractActionType() {
    override val identifier: String = "block-place"
    override fun getTranslationType(): String = "block"

    override fun rollback(world: ServerWorld): Boolean {
        world.setBlockState(pos, Blocks.AIR.defaultState)

        return true
    }
}