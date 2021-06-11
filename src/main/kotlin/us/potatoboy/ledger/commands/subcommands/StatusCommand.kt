package us.potatoboy.ledger.commands.subcommands

import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.CommandManager
import net.minecraft.text.ClickEvent
import net.minecraft.text.TranslatableText
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.commands.CommandConsts
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode
import us.potatoboy.ledger.utility.TextColorPallet
import us.potatoboy.ledger.utility.literal
import us.potatoboy.ledger.utility.translate

object StatusCommand : BuildableCommand {
    override fun build(): LiteralNode =
        CommandManager.literal("status")
            .requires(Permissions.require("ledger.commands.status", CommandConsts.PERMISSION_LEVEL))
            .executes { status(it) }
            .build()

    private fun status(context: Context): Int {
        Ledger.launch {
            val source = context.source
            source.sendFeedback(
                TranslatableText("text.ledger.header.status")
                    .setStyle(TextColorPallet.primary),
                false
            )
            source.sendFeedback(
                TranslatableText(
                    "text.ledger.status.queue",
                    if (DatabaseManager.dbMutex.isLocked) {
                        "text.ledger.status.queue.busy".translate().setStyle(TextColorPallet.secondaryVariant)
                    } else {
                        "text.ledger.status.queue.empty".translate().setStyle(TextColorPallet.secondaryVariant)
                    }
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
            source.sendFeedback(
                TranslatableText(
                    "text.ledger.status.discord",
                    "text.ledger.status.discord.join".translate()
                        .setStyle(TextColorPallet.secondaryVariant)
                        .styled {
                            it.withClickEvent(
                                ClickEvent(
                                    ClickEvent.Action.OPEN_URL,
                                    "https://discord.gg/FpRNYrQaGP"
                                )
                            )
                        }
                ).setStyle(TextColorPallet.secondary), false
            )
            source.sendFeedback(
                TranslatableText(
                    "text.ledger.status.wiki",
                    "text.ledger.status.wiki.view".translate()
                        .setStyle(TextColorPallet.secondaryVariant)
                        .styled {
                            it.withClickEvent(
                                ClickEvent(
                                    ClickEvent.Action.OPEN_URL,
                                    "https://quiltservertools.github.io/Ledger/${getVersion().friendlyString}/"
                                )
                            )
                        }
                ).setStyle(TextColorPallet.secondary), false
            )
        }

        return 1
    }

    private fun getVersion() =
        FabricLoader.getInstance().getModContainer(Ledger.MOD_ID).get().metadata.version
}
