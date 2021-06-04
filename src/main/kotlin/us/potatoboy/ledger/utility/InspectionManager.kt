package us.potatoboy.ledger.utility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.SearchResults
import us.potatoboy.ledger.database.DatabaseManager
import java.util.UUID
import kotlin.collections.HashSet

private val inspectingUsers = HashSet<UUID>()

fun ServerPlayerEntity.isInspecting() = inspectingUsers.contains(this.uuid)

fun ServerPlayerEntity.inspectOn(): Int {
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

fun ServerPlayerEntity.inspectOff(): Int {
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

fun ServerPlayerEntity.inspectBlock(pos: BlockPos) {
    val source = this.commandSource

    Ledger.launch(Dispatchers.IO) {
        val params = ActionSearchParams.build {
            min = pos
            max = pos
            worlds = mutableSetOf(source.world.registryKey.value)
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

suspend fun ServerPlayerEntity.getInspectResults(pos: BlockPos): SearchResults {
    val source = this.commandSource
    val params = ActionSearchParams.build {
        min = pos
        max = pos
    }

    Ledger.searchCache[source.name] = params
    MessageUtils.warnBusy(source)
    return DatabaseManager.searchActions(params, 1)
}

