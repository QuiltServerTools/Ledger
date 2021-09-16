package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import java.util.UUID

private val inspectingUsers = HashSet<UUID>()

fun PlayerEntity.isInspecting() = inspectingUsers.contains(this.uuid)

fun PlayerEntity.inspectOn(): Int {
    inspectingUsers.add(this.uuid)
    this.sendMessage(
        TranslatableText(
            "text.ledger.inspect.toggle",
            "text.ledger.inspect.on".translate().formatted(Formatting.GREEN)
        ).setStyle(TextColorPallet.secondary),
        false
    )

    return 1
}

fun PlayerEntity.inspectOff(): Int {
    inspectingUsers.remove(this.uuid)
    this.sendMessage(
        TranslatableText(
            "text.ledger.inspect.toggle",
            "text.ledger.inspect.off".translate().formatted(Formatting.RED)
        ).setStyle(TextColorPallet.secondary),
        false
    )

    return 1
}

fun ServerCommandSource.inspectBlock(pos: BlockPos) {
    val source = this

    Ledger.launch(Dispatchers.IO) {
        val params = ActionSearchParams.build {
            min = pos
            max = pos
            worlds = mutableSetOf(Negatable.allow(source.world.registryKey.value))
        }

        Ledger.searchCache[source.name] = params

        MessageUtils.warnBusy(source)
        val results = DatabaseManager.searchActions(params, 1)

        if (results.actions.isEmpty()) {
            source.sendError(TranslatableText("error.ledger.command.no_results"))
            return@launch
        }

        MessageUtils.sendSearchResults(
            source,
            results,
            TranslatableText(
                "text.ledger.header.search.pos",
                "${pos.x} ${pos.y} ${pos.z}".literal()
            ).setStyle(TextColorPallet.primary)
        )
    }
}

suspend fun PlayerEntity.getInspectResults(pos: BlockPos): SearchResults {
    val source = this.commandSource
    val params = ActionSearchParams.build {
        min = pos
        max = pos
    }

    Ledger.searchCache[source.name] = params
    MessageUtils.warnBusy(source)
    return DatabaseManager.searchActions(params, 1)
}

