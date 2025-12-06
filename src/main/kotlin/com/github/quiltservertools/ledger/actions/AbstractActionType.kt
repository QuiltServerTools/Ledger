package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.Sources
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import net.minecraft.world.level.Level
import java.time.Instant
import kotlin.time.ExperimentalTime

abstract class AbstractActionType : ActionType {
    override var id: Int = -1
    override var timestamp: Instant = Instant.now()
    override var pos: BlockPos = BlockPos.ZERO
    override var world: ResourceLocation? = null
    override var objectIdentifier: ResourceLocation = ResourceLocation.withDefaultNamespace("air")
    override var oldObjectIdentifier: ResourceLocation = ResourceLocation.withDefaultNamespace("air")
    override var objectState: String? = null
    override var oldObjectState: String? = null
    override var sourceName: String = Sources.UNKNOWN
    override var sourceProfile: NameAndId? = null
    override var extraData: String? = null
    override var rolledBack: Boolean = false

    override fun rollback(server: MinecraftServer): Boolean = false
    override fun previewRollback(preview: Preview, player: ServerPlayer) = Unit
    override fun previewRestore(preview: Preview, player: ServerPlayer) = Unit
    override fun restore(server: MinecraftServer): Boolean = false

    @ExperimentalTime
    override fun getMessage(source: CommandSourceStack): Component {
        val message = Component.translatable(
            "text.ledger.action_message",
            getTimeMessage(),
            getSourceMessage(),
            getActionMessage(),
            getObjectMessage(source),
            getLocationMessage()
        )
        message.style = TextColorPallet.light

        if (rolledBack) {
            message.withStyle(ChatFormatting.STRIKETHROUGH)
        }

        return message
    }

    @ExperimentalTime
    open fun getTimeMessage(): Component = MessageUtils.instantToText(timestamp)

    open fun getSourceMessage(): Component {
        if (sourceProfile == null) {
            return "@$sourceName".literal().setStyle(TextColorPallet.secondary)
        }

        if (sourceName == Sources.PLAYER) {
            return sourceProfile!!.name.literal().setStyle(TextColorPallet.secondary)
        }

        return "@$sourceName (${sourceProfile!!.name})".literal().setStyle(TextColorPallet.secondary)
    }

    open fun getActionMessage(): Component = Component.translatable("text.ledger.action.$identifier")
        .withStyle {
            it.withHoverEvent(
                HoverEvent.ShowText(
                    identifier.literal()
                )
            )
        }

    open fun getObjectMessage(source: CommandSourceStack): Component = Component.translatable(
        Util.makeDescriptionId(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).withStyle {
        it.withHoverEvent(
            HoverEvent.ShowText(
                objectIdentifier.toString().literal()
            )
        )
    }

    open fun getLocationMessage(): Component = "${pos.x} ${pos.y} ${pos.z}".literal()
        .setStyle(TextColorPallet.secondary)
        .withStyle {
            it.withHoverEvent(
                HoverEvent.ShowText(
                    Component.literal(world?.let { "$it\n" } ?: "")
                        .append(Component.translatable("text.ledger.action_message.location.hover"))
                )
            ).withClickEvent(
                ClickEvent.RunCommand(
                    "/lg tp ${world ?: Level.OVERWORLD.location()} ${pos.x} ${pos.y} ${pos.z}"
                )
            )
        }
}
