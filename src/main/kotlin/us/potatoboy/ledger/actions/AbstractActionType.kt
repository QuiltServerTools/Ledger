package us.potatoboy.ledger.actions

import com.mojang.authlib.GameProfile
import net.minecraft.block.BlockState
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.literal
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

abstract class AbstractActionType : ActionType {
    override var timestamp: Instant = Instant.now()
    override var pos: BlockPos = BlockPos.ORIGIN
    override var world: Identifier? = null
    override var objectIdentifier: Identifier = Identifier("air")
    override var oldObjectIdentifier: Identifier = Identifier("air")
    override var blockState: BlockState? = null
    override var oldBlockState: BlockState? = null
    override var sourceName: String = "Unknown"
    override var sourceProfile: GameProfile? = null
    override var extraData: String? = null
    override var rolledBack: Boolean = false

    override fun rollback(world: ServerWorld): Boolean = false
    override fun preview(world: ServerWorld, player: ServerPlayerEntity) = Unit
    override fun restore(world: ServerWorld): Boolean = false

    @ExperimentalTime
    override fun getMessage(): Text {
        val message = TranslatableText(
            "text.ledger.action_message",
            getTimeMessage(),
            getSourceMessage(),
            getActionMessage(),
            getObjectMessage(),
            getLocationMessage()
        )

        if (rolledBack) {
            message.formatted(Formatting.STRIKETHROUGH)
        }

        return message
    }

    @ExperimentalTime
    open fun getTimeMessage(): Text {
        val duration = Duration.between(timestamp, Instant.now()).toKotlinDuration()
        val message: MutableText = "".literal()

        duration.toComponents { days, hours, minutes, seconds, _ ->

            when {
                days > 0 -> message.append(days.toString()).append("d")
                hours > 0 -> message.append(hours.toString()).append("h")
                minutes > 0 -> message.append(minutes.toString()).append("m")
                else -> message.append(seconds.toString()).append("s")
            }
        }

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        val timeMessage = formatter.format(timestamp.atZone(TimeZone.getDefault().toZoneId())).literal()

        message.styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    timeMessage
                )
            )
        }

        return message
    }

    open fun getSourceMessage(): Text = if (sourceProfile != null) {
        sourceProfile!!.name.literal().setStyle(TextColorPallet.secondary)
    } else {
        "@$sourceName".literal().setStyle(TextColorPallet.secondary)
    }

    open fun getActionMessage(): Text = TranslatableText("text.ledger.action.$identifier")
        .styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    identifier.literal()
                )
            )
        }

    open fun getObjectMessage(): Text = TranslatableText(
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
                    TranslatableText("text.ledger.action_message.location.hover")
                )
            ).withClickEvent(
                ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/lg tp ${world ?: World.OVERWORLD.value} ${pos.x} ${pos.y} ${pos.z}"
                )
            )
        }
}
