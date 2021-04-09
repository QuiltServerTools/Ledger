package us.potatoboy.ledger.actionutils

import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import us.potatoboy.ledger.actions.*

object ActionFactory {
    fun blockBreakAction(world: World, pos: BlockPos, state: BlockState, source: String): BlockBreakActionType {
        val action = BlockBreakActionType()
        setBlockData(action, pos, world, Blocks.AIR.defaultState, state, source)

        return action
    }

    fun blockBreakAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: ServerPlayerEntity
    ): BlockChangeActionType {
        val action = blockBreakAction(world, pos, state, "player")
        action.sourceProfile = source.gameProfile

        return action
    }

    fun blockPlaceAction(world: World, pos: BlockPos, state: BlockState, source: String): BlockChangeActionType {
        val action = BlockChangeActionType("block-place")
        setBlockData(action, pos, world, state, Blocks.AIR.defaultState, source)

        return action
    }

    fun blockPlaceAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: ServerPlayerEntity
    ): BlockChangeActionType {
        val action = blockPlaceAction(world, pos, state, "player")
        action.sourceProfile = source.gameProfile

        return action
    }

    private fun setBlockData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        state: BlockState,
        oldState: BlockState,
        source: String
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.BLOCK.getId(state.block)
        action.oldObjectIdentifier = Registry.BLOCK.getId(oldState.block)
        action.blockState = state
        action.oldBlockState = oldState
        action.sourceName = source
        val entity = if (oldState.block.hasBlockEntity()) world.getBlockEntity(pos) else null
        action.extraData = entity?.toTag(CompoundTag())?.asString()
    }

    fun itemInsertAction(world: World, stack: ItemStack, pos: BlockPos, source: String): ItemInsertActionType {
        val action = ItemInsertActionType()
        setItemData(action, pos, world, stack, source)

        return action
    }

    fun itemInsertAction(
        world: World,
        stack: ItemStack,
        pos: BlockPos,
        source: ServerPlayerEntity
    ): ItemInsertActionType {
        val action = ItemInsertActionType()
        setItemData(action, pos, world, stack, "player")
        action.sourceProfile = source.gameProfile

        return action
    }

    fun itemRemoveAction(world: World, stack: ItemStack, pos: BlockPos, source: String): ItemRemoveActionType {
        val action = ItemRemoveActionType()
        setItemData(action, pos, world, stack, source)

        return action
    }

    fun itemRemoveAction(
        world: World,
        stack: ItemStack,
        pos: BlockPos,
        source: ServerPlayerEntity
    ): ItemRemoveActionType {
        val action = ItemRemoveActionType()
        setItemData(action, pos, world, stack, "player")
        action.sourceProfile = source.gameProfile

        return action
    }

    private fun setItemData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        stack: ItemStack,
        source: String
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.ITEM.getId(stack.item)
        action.sourceName = source
        action.extraData = stack.toTag(CompoundTag())?.asString()
    }
}