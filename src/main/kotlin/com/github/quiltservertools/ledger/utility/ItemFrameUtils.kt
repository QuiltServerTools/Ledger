package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actions.ItemChangeActionType
import com.github.quiltservertools.ledger.actions.ItemInsertActionType
import com.github.quiltservertools.ledger.actions.ItemRemoveActionType
import com.github.quiltservertools.ledger.actionutils.ActionFactory
import net.minecraft.entity.decoration.ItemFrameEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.StringNbtReader
import net.minecraft.registry.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun ItemChangeActionType.isItemFrame() = Registries.ENTITY_TYPE.getOrEmpty(oldObjectIdentifier).isPresent
fun ItemChangeActionType.itemFrameAddItem(server: MinecraftServer): Boolean {
    val world = server.getWorld(world) ?: return false
    val type = Registries.ENTITY_TYPE.getOrEmpty(oldObjectIdentifier)
    if (type.isPresent) {
        val nbt = StringNbtReader.parse(extraData)
        if (nbt.containsUuid("UUID")) {
            val uuid = nbt.getUuid("UUID")
            val entity = world.getEntity(uuid) as ItemFrameEntity?

            if (entity != null) {
                val rollbackEntity = type.get().create(world) as ItemFrameEntity
                rollbackEntity.readNbt(nbt)
                val rollbackItemStack = rollbackEntity.heldItemStack
                entity.heldItemStack = rollbackItemStack
                return true
            }

        }
    }

    return false
}

fun ItemChangeActionType.itemFrameRemoveItem(server: MinecraftServer): Boolean {
    val world = server.getWorld(world) ?: return false
    if (isItemFrame()) {
        val nbt = StringNbtReader.parse(extraData)
        if (nbt.containsUuid("UUID")) {
            val uuid = nbt.getUuid("UUID")
            val entity = world.getEntity(uuid) as ItemFrameEntity?

            if (entity != null) {
                entity.heldItemStack = ItemStack.EMPTY
                return true
            }

        }
    }

    return false
}

fun ItemChangeActionType.getItemFrameObjectMessage(): Text {
    val nbt = StringNbtReader.parse(extraData)
    val nbtCompound: NbtCompound = nbt.getCompound("Item")
    val stack = ItemStack.fromNbt(nbtCompound)

    return "${stack.count} ".literal().append(
        Text.translatable(
            Util.createTranslationKey(
                getTranslationType(),
                objectIdentifier
            )
        )
    ).setStyle(TextColorPallet.secondaryVariant).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_ITEM,
                HoverEvent.ItemStackContent(stack)
            )
        )
    }
}

fun ActionFactory.itemInsertAction(
    world: World,
    stack: ItemStack,
    pos: BlockPos,
    source: String,
    player: PlayerEntity?,
    itemFrameEntity: ItemFrameEntity
): ItemInsertActionType {
    val action = ItemInsertActionType()
    setItemData(action, pos, world, stack, source, itemFrameEntity)
    if (player != null) action.sourceProfile = player.gameProfile

    return action
}

fun ActionFactory.itemRemoveAction(
    world: World,
    stack: ItemStack,
    pos: BlockPos,
    source: String,
    player: PlayerEntity?,
    itemFrameEntity: ItemFrameEntity
): ItemRemoveActionType {
    val action = ItemRemoveActionType()
    setItemData(action, pos, world, stack, source, itemFrameEntity)
    if (player != null) action.sourceProfile = player.gameProfile

    return action
}

fun ActionFactory.setItemData(
    action: ActionType,
    pos: BlockPos,
    world: World,
    stack: ItemStack,
    source: String,
    itemFrameEntity: ItemFrameEntity
) {
    action.pos = pos
    action.world = world.registryKey.value
    action.objectIdentifier = Registries.ITEM.getId(stack.item)
    action.oldObjectIdentifier = Registries.ENTITY_TYPE.getId(itemFrameEntity.type)
    action.sourceName = source
    action.extraData = itemFrameEntity.writeNbt(NbtCompound()).asString()
}
