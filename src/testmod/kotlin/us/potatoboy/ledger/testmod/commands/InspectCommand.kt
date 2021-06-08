package us.potatoboy.ledger.testmod.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource
import net.minecraft.text.LiteralText

object InspectCommand {
    var inspectOn = false

    fun build(): LiteralCommandNode<FabricClientCommandSource> =
        literal("inspect")
            .executes {
                inspectOn = !inspectOn
                if (inspectOn) {
                    it.source.sendFeedback(LiteralText("Enabled client-side inspect."))
                } else {
                    it.source.sendFeedback(LiteralText("Disabled client-side inspect."))
                }
                1
            }
            .then(
                literal("on")
                    .executes {
                        inspectOn = true
                        it.source.sendFeedback(LiteralText("Enabled client-side inspect."))
                        1
                    }
            )
            .then(
                literal("off")
                    .executes {
                        inspectOn = false
                        it.source.sendFeedback(LiteralText("Disabled client-side inspect."))
                        1
                    }
            )
            .build()


}
