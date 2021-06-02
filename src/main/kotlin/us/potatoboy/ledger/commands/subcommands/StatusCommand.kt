package us.potatoboy.ledger.commands.subcommands

import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.CommandManager
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.database.DatabaseQueue
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.literal

object StatusCommand : BuildableCommand {
    override fun build(): LiteralNode =
        CommandManager.literal("status")
            .requires(Permissions.require("ledger.commands.status", Ledger.PERMISSION_LEVEL))
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
                DatabaseQueue.size().toString().literal()
                    .setStyle(TextColorPallet.secondaryVariant)
            ).setStyle(TextColorPallet.secondary),
            false
        )
        source.sendFeedback(
            TranslatableText(
                "text.ledger.status.version",
                getVersion().friendlyString.literal()
                    .setStyle(TextColorPallet.secondaryVariant)
            ).setStyle(TextColorPallet.secondary),
            false
        )

        return 1
    }

    private fun getVersion() =
        FabricLoader.getInstance().getModContainer(Ledger.MOD_ID).get().metadata.version
}
