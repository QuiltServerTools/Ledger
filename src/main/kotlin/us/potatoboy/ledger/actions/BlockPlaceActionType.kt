package us.potatoboy.ledger.actions

import net.minecraft.block.Blocks
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

class BlockPlaceActionType() : AbstractActionType() {
    override val identifier: String = "block-place"
    override fun getTranslationType(): String = "block"

    override fun rollback(world: ServerWorld): Boolean {
        world.setBlockState(pos, Blocks.AIR.defaultState)

        return true
    }

    override fun preview(world: ServerWorld, player: ServerPlayerEntity) {
        player.networkHandler.sendPacket(BlockUpdateS2CPacket(pos, Blocks.AIR.defaultState))
    }
}