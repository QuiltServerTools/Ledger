package us.potatoboy.ledger.actions

import net.minecraft.block.BlockState
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

object ActionFactory {
    fun blockBreakAction(world: World, pos: BlockPos, state: BlockState, source: String): BlockBreakActionType {
        val action = BlockBreakActionType()
        setBlockData(action, pos, world, state, source)

        return action
    }

    fun blockBreakAction(world: World, pos: BlockPos, state: BlockState, source: ServerPlayerEntity): BlockBreakActionType {
        val action = blockBreakAction(world, pos, state, source.name.asString())
        action.sourceProfile = source.gameProfile

        return action
    }

    fun blockPlaceAction(world: World, pos: BlockPos, state: BlockState, source: String): BlockPlaceActionType {
        val action = BlockPlaceActionType()
        setBlockData(action, pos, world, state, source)

        return action
    }

    fun blockPlaceAction(world: World, pos: BlockPos, state: BlockState, source: ServerPlayerEntity): BlockPlaceActionType {
        val action = blockPlaceAction(world, pos, state, source.name.asString())
        action.sourceProfile = source.gameProfile

        return action
    }

    private fun setBlockData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        state: BlockState,
        source: String
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.BLOCK.getId(state.block)
        action.blockState = state
        action.sourceName = source
        val entity = world.getBlockEntity(pos)
        action.extraData = entity?.toTag(CompoundTag())?.asString()
    }
}