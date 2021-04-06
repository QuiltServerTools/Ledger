package us.potatoboy.ledger.actions

import com.mojang.authlib.GameProfile
import net.minecraft.block.BlockState
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.utility.appendWithSpace
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinDuration

abstract class AbstractActionType : ActionType {
    override var timestamp: Instant = Instant.now()
    override var pos: BlockPos = BlockPos.ORIGIN
    override var world: Identifier? = null
    override var objectIdentifier: Identifier = Identifier("air")
    override var blockState: BlockState? = null
    override var sourceName: String = "Unknown"
    override var sourceProfile: GameProfile? = null
    override var extraData: String? = null
    override var rolledBack: Boolean = false

    override fun rollback(world: ServerWorld): Boolean = false
    override fun preview(world: ServerWorld, player: ServerPlayerEntity) {}

    @ExperimentalTime
    override fun getMessage(): Text {
        val message: MutableText = LiteralText("")
        message.appendWithSpace(getSourceMessage())
        message.appendWithSpace(getActionMessage())
        message.appendWithSpace(getObjectMessage())
        message.appendWithSpace(getTimeMessage())
        message.append(getLocationMessage())

        if (rolledBack) {
            message.formatted(Formatting.STRIKETHROUGH)
        }

        return message
    }

    @ExperimentalTime
    open fun getTimeMessage(): Text {
        val duration = Duration.between(timestamp, Instant.now()).toKotlinDuration()
        val text: MutableText = LiteralText("")

        duration.toComponents { days, hours, minutes, seconds, nanoseconds ->

            when {
                days > 0 -> {
                    text.append(days.toString()).append("d")
                }
                hours > 0 -> {
                    text.append(hours.toString()).append("h")
                }
                minutes > 0 -> {
                    text.append(minutes.toString()).append("m")
                }
                else -> {
                    text.append(seconds.toString()).append("s")
                }
            }
        }

        val message = TranslatableText("text.ledger.action_message.time_diff", text)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        val timeMessage = LiteralText(formatter.format(timestamp.atZone(TimeZone.getDefault().toZoneId())))

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
        LiteralText(sourceProfile!!.name).setStyle(TextColorPallet.secondary)
    } else {
        LiteralText("@$sourceName").setStyle(TextColorPallet.secondary)
    }

    open fun getActionMessage(): Text = TranslatableText("text.ledger.action.${identifier}")
        .styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    LiteralText(identifier)
                )
            )
        }

    open fun getObjectMessage(): Text = TranslatableText(
        Util.createTranslationKey(
            this.getTranslationType(),
            objectIdentifier
        )
    ).setStyle(TextColorPallet.tertiary).styled {
        it.withHoverEvent(
            HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                LiteralText(objectIdentifier.toString())
            )
        )
    }

    open fun getLocationMessage(): Text = LiteralText("${pos.x} ${pos.y} ${pos.z}")
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
                    "/tp ${pos.x} ${pos.y} ${pos.z}"
                )
            )
        }
}