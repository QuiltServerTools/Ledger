package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.database.Tables
import net.minecraft.text.Text
import org.ktorm.dsl.QueryRowSet
import java.time.Instant
import java.util.*
import kotlin.time.ExperimentalTime

data class PlayerResult(val uuid: UUID, val name: String, val firstJoin: Instant, val lastJoin: Instant) {

    @OptIn(ExperimentalTime::class)
    fun toText(): Text {
        return Text.translatable(
            "text.ledger.player.result",
            name.literal().setStyle(TextColorPallet.light),
            MessageUtils.instantToText(firstJoin).setStyle(TextColorPallet.primaryVariant),
            MessageUtils.instantToText(lastJoin).setStyle(TextColorPallet.primaryVariant)
        ).setStyle(TextColorPallet.secondary)
    }

    companion object {
        fun fromRow(row: QueryRowSet): PlayerResult = PlayerResult(
            row[Tables.Players.playerId]!!,
            row[Tables.Players.playerName]!!,
            row[Tables.Players.firstJoin] ?: Instant.now(),
            row[Tables.Players.lastJoin] ?: Instant.now()
        )
    }
}
