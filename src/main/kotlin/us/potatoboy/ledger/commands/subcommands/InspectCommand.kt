package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.MessageUtils
import us.potatoboy.ledger.utility.literal

object InspectCommand : BuildableCommand {
    override fun build(): LiteralNode =
        literal("inspect")
            .executes { toggleInspect(it) }
            .then(
                argument("pos", BlockPosArgumentType.blockPos())
                    .executes { inspectBlock(it, BlockPosArgumentType.getBlockPos(it, "pos")) }
            )
            .build()


    private fun toggleInspect(context: Context): Int {
        val source = context.source
        val player = source.player

        //TODO inspect mode
        source.sendFeedback(LiteralText("WIP"), false)
        return 1
    }

    private fun inspectBlock(context: Context, pos: BlockPos): Int {
        val source = context.source

        GlobalScope.launch(Dispatchers.IO) {
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
        return 1
    }
}