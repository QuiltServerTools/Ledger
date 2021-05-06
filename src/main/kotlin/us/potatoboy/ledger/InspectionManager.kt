package us.potatoboy.ledger

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.MessageUtils
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.literal
import us.potatoboy.ledger.utility.translate
import java.util.*

object InspectionManager {
    private val inspectingUsers = HashSet<UUID>()

    fun inspectOn(player: ServerPlayerEntity): Int {
        inspectingUsers.add(player.uuid)
        player.sendMessage(
            TranslatableText("text.ledger.inspect.toggle",
                "text.ledger.inspect.on".translate().formatted(Formatting.GREEN)
            ).setStyle(TextColorPallet.secondary),
            false
        )

        return 1
    }

    fun inspectOff(player: ServerPlayerEntity): Int {
        inspectingUsers.remove(player.uuid)
        player.sendMessage(
            TranslatableText("text.ledger.inspect.toggle",
                "text.ledger.inspect.off".translate().formatted(Formatting.RED)
            ).setStyle(TextColorPallet.secondary),
            false
        )

        return 1
    }

    fun isInspecting(player: ServerPlayerEntity): Boolean = inspectingUsers.contains(player.uuid)

    fun inspectBlock(player: ServerPlayerEntity, pos: BlockPos) {
        val source = player.commandSource

        runBlocking {
            launch(Dispatchers.IO) {
                val params = ActionSearchParams(
                    min = pos,
                    max = pos,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )

                Ledger.searchCache[source.name] = params

                val results = DatabaseManager.searchActions(params, 1, source)

                if (results.actions.isEmpty()) {
                    source.sendError(TranslatableText("error.ledger.command.no_results"))
                    return@launch
                }

                MessageUtils.sendSearchResults(
                    source, results,
                    TranslatableText(
                        "text.ledger.header.search.pos",
                        "${pos.x} ${pos.y} ${pos.z}".literal()
                        //.setStyle(TextColorPallet.secondary)
                    ).setStyle(TextColorPallet.primary)
                )

            }
        }
    }
}