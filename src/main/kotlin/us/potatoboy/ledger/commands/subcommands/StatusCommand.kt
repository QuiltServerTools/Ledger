package us.potatoboy.ledger.commands.subcommands

import net.minecraft.server.command.CommandManager
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.TextColorPallet
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.database.ActionQueue
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

object StatusCommand : BuildableCommand {
    override fun build(): LiteralNode =
        CommandManager.literal("status")
            .executes { status(it) }
            .build()

    private fun status(context: Context): Int {
        val source = context.source
        source.sendFeedback(
            TranslatableText("text.ledger.header.status")
                .setStyle(TextColorPallet.primary),
            false
        )
        source.sendFeedback(
            TranslatableText(
                "text.ledger.status.queue",
                LiteralText(
                    ActionQueue.size().toString()
                ).setStyle(TextColorPallet.primary)
            ).setStyle(TextColorPallet.secondary),
            false
        )

        return 1
    }
}