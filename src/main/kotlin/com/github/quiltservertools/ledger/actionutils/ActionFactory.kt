package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.*
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
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
        action.objectIdentifier = Registries.BLOCK.getId(state.block)
        action.oldObjectIdentifier = Registries.BLOCK.getId(oldState.block)
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

    fun itemPickUpAction(
        world: World,
        pos: BlockPos,
        stack: ItemStack,
        source: PlayerEntity
    ): ItemPickUpActionType {
        val action = ItemPickUpActionType()
        setItemData(action, pos, world, stack, Sources.PLAYER)
        action.sourceProfile = source.gameProfile

        return action
    }

    fun itemDropAction(
        world: World,
        pos: BlockPos,
        stack: ItemStack,
        source: PlayerEntity
    ): ItemDropActionType {
        val action = ItemDropActionType()
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
        action.objectIdentifier = Registries.ITEM.getId(stack.item)
        action.sourceName = source
        action.extraData = NbtUtils.itemToProperties(stack)?.asString()
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
                val source = Registries.ENTITY_TYPE.getId(killer.type).path
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
        action.objectIdentifier = Registries.ENTITY_TYPE.getId(entity.type)
        action.sourceName = source
        action.extraData = entity.writeNbt(NbtCompound())?.asString()
    }

    fun entityChangeAction(
        world: World,
        pos: BlockPos,
        oldEntityTags: NbtCompound,
        entity: Entity,
        itemStack: ItemStack?,
        entityActor: Entity?,
        sourceType: String
    ): EntityChangeActionType {
        val action = EntityChangeActionType()

        action.pos = pos
        action.world = world.registryKey.value
        action.objectIdentifier = Registry.ENTITY_TYPE.getId(entity.type)
        action.oldObjectIdentifier = Registry.ENTITY_TYPE.getId(entity.type)

        if (itemStack != null) {
            action.extraData = Registry.ITEM.getId(itemStack.item).toString()
        }
        action.oldEntityState = oldEntityTags.asString()
        action.entityState = entity.writeNbt(NbtCompound())?.asString()
        action.sourceName = sourceType

        if (entityActor is PlayerEntity) {
            action.sourceProfile = entityActor.gameProfile
        }

        return action
    }
}
