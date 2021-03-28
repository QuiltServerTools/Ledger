package us.potatoboy.ledger.actions

import com.mojang.authlib.GameProfile
import net.minecraft.block.BlockState
import net.minecraft.text.*
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

    override fun rollback(): Boolean = false

    @ExperimentalTime
    override fun getMessage(): Text {
        val message: MutableText = LiteralText("")
        if (sourceProfile != null) {
            message.appendWithSpace(LiteralText(sourceProfile!!.name).setStyle(TextColorPallet.secondary))
        } else {
            message.appendWithSpace(LiteralText("@$sourceName").setStyle(TextColorPallet.secondary))
        }

        message.appendWithSpace(TranslatableText("text.ledger.action.${identifier}").styled {
            it.withHoverEvent(
                HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    LiteralText(identifier)
                )
            )
        })

        message.appendWithSpace(
            TranslatableText(
                Util.createTranslationKey(
                    this.getTranslationType(),
                    objectIdentifier
                )
            ).setStyle(TextColorPallet.tertiary)
        )

        message.appendWithSpace(getTimeDiff())

        return message
    }

    @ExperimentalTime
    private fun getTimeDiff(): Text {
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
}