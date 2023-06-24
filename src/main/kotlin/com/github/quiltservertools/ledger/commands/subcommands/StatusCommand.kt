package com.github.quiltservertools.ledger.commands.subcommands

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.api.ExtensionManager
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.ledger.database.ActionQueueService
import com.github.quiltservertools.ledger.utility.Context
import com.github.quiltservertools.ledger.utility.LiteralNode
import com.github.quiltservertools.ledger.utility.TextColorPallet
import com.github.quiltservertools.ledger.utility.literal
import com.github.quiltservertools.ledger.utility.translate
import kotlinx.coroutines.launch
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.SemanticVersion
import net.minecraft.server.command.CommandManager
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

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
                {
                    Text.translatable("text.ledger.header.status")
                        .setStyle(TextColorPallet.primary)
                },
                false
            )
            source.sendFeedback(
                {
                    Text.translatable(
                        "text.ledger.status.queue",
                        ActionQueueService.getQueueSize().toString().literal()
                            .setStyle(TextColorPallet.secondaryVariant)
                    ).setStyle(TextColorPallet.secondary)
                },
                false
            )
            source.sendFeedback(
                {
                    Text.translatable(
                        "text.ledger.status.version",
                        getVersion().friendlyString.literal()
                            .setStyle(TextColorPallet.secondaryVariant)
                    ).setStyle(TextColorPallet.secondary)
                },
                false
            )
            val dbType = if (ExtensionManager.getDatabaseExtensionOptional().isPresent) {
                ExtensionManager.getDatabaseExtensionOptional().get().getIdentifier()
            } else {
                Ledger.identifier(Ledger.DEFAULT_DATABASE)
            }
            source.sendFeedback(
                {
                    Text.translatable(
                        "text.ledger.status.db_type",
                        dbType.path.literal()
                            .setStyle(TextColorPallet.secondaryVariant)
                    ).setStyle(TextColorPallet.secondary)
                },
                false
            )
            source.sendFeedback(
                {
                    Text.translatable(
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
                    ).setStyle(TextColorPallet.secondary)
                }, false
            )
            source.sendFeedback(
                {
                    Text.translatable(
                        "text.ledger.status.wiki",
                        "text.ledger.status.wiki.view".translate()
                            .setStyle(TextColorPallet.secondaryVariant)
                            .styled {
                                it.withClickEvent(
                                    ClickEvent(
                                        ClickEvent.Action.OPEN_URL,
                                        "https://quiltservertools.github.io/Ledger/latest/"
                                    )
                                )
                            }
                    ).setStyle(TextColorPallet.secondary)
                }, false
            )
        }

        return 1
    }

    private fun getVersion() = SemanticVersion.parse(
        FabricLoader.getInstance().getModContainer(Ledger.MOD_ID).get().metadata.version.friendlyString
    )
}
