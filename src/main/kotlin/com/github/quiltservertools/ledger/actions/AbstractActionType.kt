package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.Sources
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import com.mojang.authlib.GameProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.time.Instant
import kotlin.time.ExperimentalTime

abstract class AbstractActionType : ActionType {
    override var timestamp: Instant = Instant.now()
    override var pos: BlockPos = BlockPos.ORIGIN
    override var world: Identifier? = null
    override var objectIdentifier: Identifier = Identifier.ofVanilla("air")
    override var oldObjectIdentifier: Identifier = Identifier.ofVanilla("air")
    override var objectState: String? = null
    override var oldObjectState: String? = null
    override var sourceName: String = Sources.UNKNOWN
    override var sourceProfile: GameProfile? = null
    override var extraData: String? = null
    override var rolledBack: Boolean = false

    override fun rollback(server: MinecraftServer): Boolean = false
    override fun previewRollback(preview: Preview, player: ServerPlayerEntity) = Unit
    override fun previewRestore(preview: Preview, player: ServerPlayerEntity) = Unit
    override fun restore(server: MinecraftServer): Boolean = false

    @ExperimentalTime
    override fun getMessage(source: ServerCommandSource): Text {
        val message = Text.translatable(
            "text.ledger.action_message",
            getTimeMessage(),
            getSourceMessage(),
            getActionMessage(),
            getObjectMessage(source),
            getLocationMessage()
        )
        message.style = TextColorPallet.light

        if (rolledBack) {
            message.formatted(Formatting.STRIKETHROUGH)
        }

        return message
    }

    @ExperimentalTime
    open fun getTimeMessage(): Text = MessageUtils.instantToText(timestamp)

    open fun getSourceMessage(): Text {
        if (sourceProfile == null) {
            return "@$sourceName".literal().setStyle(TextColorPallet.secondary)
        }

        if (sourceName == Sources.PLAYER) {
            return sourceProfile!!.name.literal().setStyle(TextColorPallet.secondary)
        }

        return "@$sourceName (${sourceProfile!!.name})".literal().setStyle(TextColorPallet.secondary)
    }

    open fun getActionMessage(): Text = Text.translatable("text.ledger.action.$identifier")
        .styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    identifier.literal()
                )
            )
        }

    open fun getObjectMessage(source: ServerCommandSource): Text = Text.translatable(
        Util.createTranslationKey(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.secondaryVariant).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                objectIdentifier.toString().literal()
            )
        )
    }

    open fun getLocationMessage(): Text = "${pos.x} ${pos.y} ${pos.z}".literal()
        .setStyle(TextColorPallet.secondary)
        .styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.literal(world?.let { "$it\n" } ?: "")
                        .append(Text.translatable("text.ledger.action_message.location.hover"))
                )
            ).withClickEvent(
                ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/lg tp ${world ?: World.OVERWORLD.value} ${pos.x} ${pos.y} ${pos.z}"
                )
            )
        }
}
