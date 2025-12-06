package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actions.BlockBreakActionType
import com.github.quiltservertools.ledger.actions.BlockChangeActionType
import com.github.quiltservertools.ledger.actions.BlockPlaceActionType
import com.github.quiltservertools.ledger.actions.EntityChangeActionType
import com.github.quiltservertools.ledger.actions.EntityDismountActionType
import com.github.quiltservertools.ledger.actions.EntityKillActionType
import com.github.quiltservertools.ledger.actions.EntityMountActionType
import com.github.quiltservertools.ledger.actions.ItemDropActionType
import com.github.quiltservertools.ledger.actions.ItemInsertActionType
import com.github.quiltservertools.ledger.actions.ItemPickUpActionType
import com.github.quiltservertools.ledger.actions.ItemRemoveActionType
import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.NbtUtils.createNbt
import com.github.quiltservertools.ledger.utility.Sources
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.coppergolem.CopperGolem
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

object ActionFactory {
    fun blockBreakAction(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        source: String,
        entity: BlockEntity? = null
    ): BlockBreakActionType {
        val action = BlockBreakActionType()
        setBlockData(action, pos, world, Blocks.AIR.defaultBlockState(), state, source, entity)

        return action
    }

    fun blockBreakAction(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        player: Player,
        entity: BlockEntity? = null,
        source: String = Sources.PLAYER
    ): BlockChangeActionType {
        val action = blockBreakAction(world, pos, state, source, entity)
        action.sourceProfile = player.nameAndId()

        return action
    }

    fun blockPlaceAction(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        source: String,
        entity: BlockEntity? = null
    ): BlockChangeActionType {
        val action = BlockPlaceActionType()
        setBlockData(action, pos, world, state, Blocks.AIR.defaultBlockState(), source, entity)

        return action
    }

    fun blockPlaceAction(
        world: Level,
        pos: BlockPos,
        state: BlockState,
        player: Player,
        entity: BlockEntity? = null,
        source: String = Sources.PLAYER
    ): BlockChangeActionType {
        val action = blockPlaceAction(world, pos, state, source, entity)
        action.sourceProfile = player.nameAndId()

        return action
    }

    private fun setBlockData(
        action: ActionType,
        pos: BlockPos,
        world: Level,
        state: BlockState,
        oldState: BlockState,
        source: String,
        entity: BlockEntity? = null
    ) {
        action.pos = pos
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.BLOCK.getKey(state.block)
        action.oldObjectIdentifier = BuiltInRegistries.BLOCK.getKey(oldState.block)
        action.objectState = NbtUtils.blockStateToProperties(state)?.toString()
        action.oldObjectState = NbtUtils.blockStateToProperties(oldState)?.toString()
        action.sourceName = source
        action.extraData = entity?.saveWithoutMetadata(world.registryAccess())?.toString()
    }

    fun itemInsertAction(world: Level, stack: ItemStack, pos: BlockPos, source: String): ItemInsertActionType {
        val action = ItemInsertActionType()
        setItemData(action, pos, world, stack, source)

        return action
    }

    fun itemInsertAction(
        world: Level,
        stack: ItemStack,
        pos: BlockPos,
        source: LivingEntity
    ): ItemInsertActionType {
        val action = ItemInsertActionType()
        var sourceType = Sources.UNKNOWN
        if (source is Player) {
            sourceType = Sources.PLAYER
            action.sourceProfile = source.nameAndId()
        } else if (source is CopperGolem) {
            sourceType = Sources.COPPER_GOLEM
            action.sourceName = sourceType
        }
        setItemData(action, pos, world, stack, sourceType)

        return action
    }

    fun itemRemoveAction(world: Level, stack: ItemStack, pos: BlockPos, source: String): ItemRemoveActionType {
        val action = ItemRemoveActionType()
        setItemData(action, pos, world, stack, source)

        return action
    }

    fun itemRemoveAction(
        world: Level,
        stack: ItemStack,
        pos: BlockPos,
        source: LivingEntity
    ): ItemRemoveActionType {
        val action = ItemRemoveActionType()
        var sourceType = Sources.UNKNOWN
        if (source is Player) {
            sourceType = Sources.PLAYER
            action.sourceProfile = source.nameAndId()
        } else if (source is CopperGolem) {
            sourceType = Sources.COPPER_GOLEM
            action.sourceName = sourceType
        }
        setItemData(action, pos, world, stack, sourceType)

        return action
    }

    fun itemPickUpAction(
        entity: ItemEntity,
        source: Player
    ): ItemPickUpActionType {
        val action = ItemPickUpActionType()

        setItemData(action, entity.blockPosition(), entity.level(), entity.item, Sources.PLAYER)

        action.oldObjectState = entity.createNbt().toString()
        action.sourceProfile = source.nameAndId()

        return action
    }

    fun itemDropAction(
        entity: ItemEntity,
        source: LivingEntity
    ): ItemDropActionType {
        val action = ItemDropActionType()

        setItemData(action, entity.blockPosition(), entity.level(), entity.item, Sources.PLAYER)

        action.objectState = entity.createNbt().toString()
        if (source is Player) {
            action.sourceProfile = source.nameAndId()
        } else if (source is CopperGolem) {
            action.sourceName = Sources.COPPER_GOLEM
        }

        return action
    }

    fun blockChangeAction(
        world: Level,
        pos: BlockPos,
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        source: String,
        player: Player?
    ): ActionType {
        val action = BlockChangeActionType()
        setBlockData(action, pos, world, newState, oldState, source, oldBlockEntity)
        action.sourceProfile = player?.nameAndId()
        return action
    }

    private fun setItemData(
        action: ActionType,
        pos: BlockPos,
        world: Level,
        stack: ItemStack,
        source: String
    ) {
        action.pos = pos
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.ITEM.getKey(stack.item)
        action.sourceName = source
        if (!stack.isEmpty) {
            action.extraData = stack.createNbt(world.registryAccess()).toString()
        }
    }

    fun entityKillAction(world: Level, pos: BlockPos, entity: Entity, cause: DamageSource): EntityKillActionType {
        val killer = cause.entity
        val action = EntityKillActionType()

        when {
            killer is Player -> {
                setEntityData(action, pos, world, entity, Sources.PLAYER)
                action.sourceProfile = killer.nameAndId()
            }

            killer != null -> {
                val source = BuiltInRegistries.ENTITY_TYPE.getKey(killer.type).path
                setEntityData(action, pos, world, entity, source)
            }

            else -> {
                setEntityData(action, pos, world, entity, cause.msgId)
            }
        }

        return action
    }

    fun entityKillAction(world: Level, pos: BlockPos, entity: Entity, source: String): EntityKillActionType {
        val action = EntityKillActionType()
        setEntityData(action, pos, world, entity, source)
        return action
    }

    private fun setEntityData(
        action: ActionType,
        pos: BlockPos,
        world: Level,
        entity: Entity,
        source: String
    ) {
        action.pos = pos
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        action.sourceName = source
        action.extraData = entity.createNbt().toString()
    }

    fun entityChangeAction(
        world: Level,
        pos: BlockPos,
        oldEntityTags: CompoundTag,
        entity: Entity,
        itemStack: ItemStack?,
        entityActor: Entity?,
        sourceType: String
    ): EntityChangeActionType {
        val action = EntityChangeActionType()

        action.pos = pos
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        action.oldObjectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)

        if (itemStack != null && !itemStack.isEmpty) {
            action.extraData = itemStack.createNbt(world.registryAccess()).toString()
        }
        action.oldObjectState = oldEntityTags.toString()
        action.objectState = entity.createNbt().toString()
        action.sourceName = sourceType

        if (entityActor is Player) {
            action.sourceProfile = entityActor.nameAndId()
        }

        return action
    }

    fun entityMountAction(
        entity: Entity,
        player: Player,
    ): EntityMountActionType {
        val world = entity.level()

        val action = EntityMountActionType()

        action.pos = entity.blockPosition()
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        action.oldObjectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)

        action.objectState = entity.createNbt().toString()
        action.sourceName = Sources.PLAYER

        action.sourceProfile = player.nameAndId()

        return action
    }

    fun entityDismountAction(
        entity: Entity,
        player: Player,
    ): EntityDismountActionType {
        val world = entity.level()

        val action = EntityDismountActionType()

        action.pos = entity.blockPosition()
        action.world = world.dimension().location()
        action.objectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)
        action.oldObjectIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type)

        action.objectState = entity.createNbt().toString()
        action.sourceName = Sources.PLAYER

        action.sourceProfile = player.nameAndId()

        return action
    }
}
