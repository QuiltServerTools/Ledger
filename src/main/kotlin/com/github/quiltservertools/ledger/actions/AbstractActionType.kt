package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.utility.MessageUtils
import com.github.quiltservertools.ledger.utility.Sources
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import net.minecraft.util.Util
import net.minecraft.world.level.Level
import java.time.Instant
import java.util.*
import kotlin.time.ExperimentalTime

private const val NO_ID = -1

abstract class AbstractActionType : ActionType {
    override var id: Int = NO_ID
    override var timestamp: Instant = Instant.now()
    override var pos: BlockPos = BlockPos.ZERO
    override var world: Identifier? = null
    override var objectIdentifier: Identifier = Identifier.withDefaultNamespace("air")
    override var oldObjectIdentifier: Identifier = Identifier.withDefaultNamespace("air")
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
            getLocationMessage(),
        )
        message.style = TextColorPallet.light

        if (rolledBack) {
            message.withStyle(ChatFormatting.STRIKETHROUGH)
        } else {
            getRollbackMessage(source)?.let { message.append(" ".literal()).append(it) }
        }

        return message
    }

    /**
     * Rollback button appended to a single search or inspect result. Only rendered for actions that
     * were read back from the database (which is where the id comes from) and are not rolled back
     * yet, and only to viewers allowed to roll back. The click is handled by
     * ServerCommonPacketListenerImplMixin, which checks the rollback permission again before acting
     * on it.
     */
    open fun getRollbackMessage(source: CommandSourceStack): Component? {
        if (id == NO_ID) return null
        if (!Permissions.check(source, "ledger.commands.rollback", CommandConsts.PERMISSION_LEVEL)) return null

        // Bound outside the apply block below, where `id` would resolve to CompoundTag's own id.
        val actionId = id

        return Component.translatable("text.ledger.action_message.rollback")
            .setStyle(TextColorPallet.primaryVariant)
            .withStyle {
                val tag: CompoundTag = CompoundTag().apply { this.putInt("id", actionId) }

                it.withHoverEvent(
                    HoverEvent.ShowText(
                        Component.translatable("text.ledger.action_message.rollback.hover"),
                    ),
                ).withClickEvent(
                    ClickEvent.Custom(MessageUtils.rollbackAction, Optional.of(tag)),
                )
            }
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
                    identifier.literal(),
                ),
            )
        }

    open fun getObjectMessage(source: CommandSourceStack): Component = Component.translatable(
        Util.makeDescriptionId(
            this.getTranslationType(),
            objectIdentifier,
        ),
    ).setStyle(TextColorPallet.secondaryVariant).withStyle {
        it.withHoverEvent(
            HoverEvent.ShowText(
                objectIdentifier.toString().literal(),
            ),
        )
    }

    open fun getLocationMessage(): Component = "${pos.x} ${pos.y} ${pos.z}".literal()
        .setStyle(TextColorPallet.secondary)
        .withStyle {
            val tag: CompoundTag = CompoundTag().apply {
                this.putInt("x", pos.x)
                this.putInt("y", pos.y)
                this.putInt("z", pos.z)
                this.putString("world", (world ?: Level.OVERWORLD.identifier()).toString())
            }

            it.withHoverEvent(
                HoverEvent.ShowText(
                    Component.literal(world?.let { "$it\n" } ?: "")
                        .append(Component.translatable("text.ledger.action_message.location.hover")),
                ),
            ).withClickEvent(
                ClickEvent.Custom(MessageUtils.teleportAction, Optional.of(tag)),
            )
        }
}
