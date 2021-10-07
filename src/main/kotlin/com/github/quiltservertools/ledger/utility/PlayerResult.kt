package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.database.Tables
import net.minecraft.text.Text
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

data class PlayerResult(val uuid: UUID, val name: String, val firstJoin: Instant, val lastJoin: Instant) {

    fun toText(): Text {
        val text = "$name ".literal().setStyle(TextColorPallet.secondaryVariant)

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)

        val firstJoinMessage = formatter.format(firstJoin.atZone(TimeZone.getDefault().toZoneId())).literal()
            .setStyle(TextColorPallet.primary)
        val lastJoinMessage = formatter.format(lastJoin.atZone(TimeZone.getDefault().toZoneId())).literal()
            .setStyle(TextColorPallet.primaryVariant)

        text.appendWithSpace(firstJoinMessage)
        text.appendWithSpace(lastJoinMessage)

        return text
    }

    companion object {
        fun fromRow(row: Tables.Player): PlayerResult = PlayerResult(row.playerId, row.playerName, row.firstJoin, row.lastJoin)
    }
}
