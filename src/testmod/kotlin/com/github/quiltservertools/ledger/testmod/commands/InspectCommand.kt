package com.github.quiltservertools.ledger.testmod.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.text.Text

object InspectCommand {
    var inspectOn = false

    fun build(): LiteralCommandNode<FabricClientCommandSource> =
        literal("inspect")
            .executes {
                inspectOn = !inspectOn
                if (inspectOn) {
                    it.source.sendFeedback(Text.literal("Enabled client-side inspect."))
                } else {
                    it.source.sendFeedback(Text.literal("Disabled client-side inspect."))
                }
                1
            }
            .then(
                literal("on")
                    .executes {
                        inspectOn = true
                        it.source.sendFeedback(Text.literal("Enabled client-side inspect."))
                        1
                    }
            )
            .then(
                literal("off")
                    .executes {
                        inspectOn = false
                        it.source.sendFeedback(Text.literal("Disabled client-side inspect."))
                        1
                    }
            )
            .build()


}
