package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.LiteralText
import net.minecraft.text.Texts
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.database.ActionLookupParams
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

class InspectCommand : BuildableCommand {
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

        source.sendFeedback(LiteralText("WIP"), false)
        return 1
    }

    private fun inspectBlock(context: Context, pos: BlockPos): Int {
        val player = context.source.player

        GlobalScope.launch(Dispatchers.IO) {
            val params = ActionLookupParams(
                min = pos,
                max = pos,
                null,
                null,
                null,
                null,
                null
            )

            val actions = DatabaseManager.searchActions(params)

            player.sendMessage(
                Texts.bracketed(
                    TranslatableText(
                        "text.ledger.header.inspect.pos",
                        LiteralText("${pos.x} ${pos.y} ${pos.z}")
                            .setStyle(TextColorPallet.secondary)
                    )
                ).setStyle(TextColorPallet.primary), false
            )
            actions.forEach { actionType ->
                player.sendMessage(actionType.getMessage(), false)
            }
        }
        return 1
    }
}