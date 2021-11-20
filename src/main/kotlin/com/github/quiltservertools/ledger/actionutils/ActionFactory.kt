package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.*
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraft.world.World

object ActionFactory {
    fun blockBreakAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: String,
        entity: BlockEntity? = null
    ): BlockBreakActionType {
        val action = BlockBreakActionType()
        setBlockData(action, pos, world, Blocks.AIR.defaultState, state, source, entity)

        return action
    }

    fun blockBreakAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        player: PlayerEntity,
        entity: BlockEntity? = null,
        source: String = Sources.PLAYER
    ): BlockChangeActionType {
        val action = blockBreakAction(world, pos, state, source, entity)
        action.sourceProfile = player.gameProfile

        return action
    }

    fun blockPlaceAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        source: String,
        entity: BlockEntity? = null
    ): BlockChangeActionType {
        val action = BlockPlaceActionType()
        setBlockData(action, pos, world, state, Blocks.AIR.defaultState, source, entity)

        return action
    }

    fun blockPlaceAction(
        world: World,
        pos: BlockPos,
        state: BlockState,
        player: PlayerEntity,
        entity: BlockEntity? = null,
        source: String = Sources.PLAYER
    ): BlockChangeActionType {
        val action = blockPlaceAction(world, pos, state, source, entity)
        action.sourceProfile = player.gameProfile

        return action
    }

    private fun setBlockData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        state: BlockState,
        oldState: BlockState,
        source: String,
        entity: BlockEntity? = null
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.BLOCK.getId(state.block)
        action.oldObjectIdentifier = Registry.BLOCK.getId(oldState.block)
        action.blockState = state
        action.oldBlockState = oldState
        action.sourceName = source
        action.extraData = entity?.createNbt()?.asString()
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
        source: PlayerEntity
    ): ItemInsertActionType {
        val action = ItemInsertActionType()
        setItemData(action, pos, world, stack, Sources.PLAYER)
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
        source: PlayerEntity
    ): ItemRemoveActionType {
        val action = ItemRemoveActionType()
        setItemData(action, pos, world, stack, Sources.PLAYER)
        action.sourceProfile = source.gameProfile

        return action
    }

    fun blockChangeAction(
        world: World,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        source: String,
        player: PlayerEntity?
    ): ActionType {
        val action = BlockChangeActionType()
        setBlockData(action, pos, world, newState, oldState, source, oldBlockEntity)
        action.sourceProfile = player?.gameProfile
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
        action.extraData = stack.writeNbt(NbtCompound())?.asString()
    }

    fun entityKillAction(world: World, pos: BlockPos, entity: Entity, cause: DamageSource): EntityKillActionType {
        val killer = cause.attacker
        val action = EntityKillActionType()

        when {
            killer is PlayerEntity -> {
                setEntityData(action, pos, world, entity, Sources.PLAYER)
                action.sourceProfile = killer.gameProfile
            }
            killer != null -> {
                val source = Registry.ENTITY_TYPE.getId(killer.type).path
                setEntityData(action, pos, world, entity, source)
            }
            else -> setEntityData(action, pos, world, entity, cause.name)
        }

        return action
    }

    private fun setEntityData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        entity: Entity,
        source: String
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.ENTITY_TYPE.getId(entity.type)
        action.sourceName = source
        action.extraData = entity.writeNbt(NbtCompound())?.asString()
    }


    private fun setEntityItemData(
        action: ActionType,
        pos: BlockPos,
        world: World,
        entity: Entity,
        itemStack: ItemStack,
        source: String
    ) {
        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.ENTITY_TYPE.getId(entity.type)
        action.oldObjectIdentifier = Registry.ITEM.getId(itemStack.item)
        action.sourceName = source
        val b = NbtCompound()
        b.put("Item",itemStack.writeNbt(NbtCompound()))
        b.put("Entity",entity.writeNbt(NbtCompound()))
        action.extraData = b.toString()


    }

    fun entityEquipAction(
        playerStack: ItemStack,
        world: World,
        pos: BlockPos,
        entity: Entity,
        source: PlayerEntity): EntityModifyActionType {

        val action = EntityModifyActionType()
        setEntityItemData(action, pos, world, entity, playerStack,"Equip")
        action.sourceProfile = source.gameProfile

        return action;
    }
    fun entityRemoveAction(
        entityStack: ItemStack,
        world: World,
        pos: BlockPos,
        entity: Entity,
        source: PlayerEntity): EntityModifyActionType {

        val action = EntityModifyActionType()
        setEntityItemData(action, pos, world, entity, entityStack,"Remove")
        action.sourceProfile = source.gameProfile

        return action;
    }
}

