package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.utility.NbtUtils
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.UUID
import com.github.quiltservertools.ledger.utility.getWorld
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.nbt.StringNbtReader
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Uuids

// TODO remove duplication from ItemPickUpActionType and ItemDropActionType
open class ItemDropActionType : AbstractActionType() {
    override val identifier = "item-drop"

    // Not used
    override fun getTranslationType(): String = "item"

    private fun getStack(server: MinecraftServer) = NbtUtils.itemFromProperties(
        extraData,
        objectIdentifier,
        server.registryManager
    )

    override fun getObjectMessage(source: ServerCommandSource): Text {
        val stack = getStack(source.server)

        return "${stack.count} ".literal().append(
            stack.itemName
        ).setStyle(TextColorPallet.secondaryVariant).styled {
            it.withHoverEvent(
                HoverEvent.ShowItem(
                    stack
                )
            )
        }
    }

    override fun rollback(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val newEntity = StringNbtReader.readCompound(objectState)
        val optionalUUID = newEntity!!.get(UUID, Uuids.INT_STREAM_CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity != null) {
            entity.remove(Entity.RemovalReason.DISCARDED)
            return true
        }
        return false
    }

    override fun restore(server: MinecraftServer): Boolean {
        val world = server.getWorld(world)

        val newEntity = StringNbtReader.readCompound(objectState)
        val optionalUUID = newEntity!!.get(UUID, Uuids.INT_STREAM_CODEC)
        if (optionalUUID.isEmpty) return false
        val entity = world?.getEntity(optionalUUID.get())

        if (entity == null) {
            val entity = ItemEntity(EntityType.ITEM, world)
            entity.readNbt(newEntity)
            world?.spawnEntity(entity)
        }
        return true
    }
}
