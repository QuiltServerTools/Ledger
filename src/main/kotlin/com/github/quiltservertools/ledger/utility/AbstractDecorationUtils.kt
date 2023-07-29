package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actions.EntityKillActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.AbstractDecorationEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun ActionFactory.entityKillAction(
    world: World,
    pos: BlockPos,
    entity: AbstractDecorationEntity,
    cause: Entity?
): EntityKillActionType {
    val action = EntityKillActionType()

    when {
        cause is PlayerEntity -> {
            ActionFactory.setEntityData(action, pos, world, entity, Sources.PLAYER)
            action.sourceProfile = cause.gameProfile
        }

        cause != null -> {
            val source = Registries.ENTITY_TYPE.getId(cause.type).path
            ActionFactory.setEntityData(action, pos, world, entity, source)
        }

        else -> ActionFactory.setEntityData(action, pos, world, entity, Sources.UNKNOWN)
    }

    return action
}

fun ActionFactory.setEntityData(
    action: ActionType,
    pos: BlockPos,
    world: World,
    entity: AbstractDecorationEntity,
    source: String
) {
    action.pos = pos
    action.world = world.registryKey.value
    action.objectIdentifier = Registries.ENTITY_TYPE.getId(entity.type)
    action.sourceName = source
    action.extraData = entity.writeNbt(NbtCompound())?.asString()
}
