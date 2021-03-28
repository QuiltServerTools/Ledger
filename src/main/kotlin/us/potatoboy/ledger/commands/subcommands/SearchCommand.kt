package us.potatoboy.ledger.commands.subcommands

import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import us.potatoboy.ledger.commands.BuildableCommand
import us.potatoboy.ledger.utility.Context
import us.potatoboy.ledger.utility.LiteralNode

class SearchCommand : BuildableCommand {
    override fun build(): LiteralNode {
        val searchNode =
            literal("search")
                .executes { search(it) }
                .build()

        val radiusNode =
            literal("radius:")
                .then(
                    argument("radius", IntegerArgumentType.integer(1))
                        .redirect(searchNode)
                )
                .build()

        searchNode.addChild(radiusNode)

        return searchNode
    }

    private fun search(context: Context): Int {
        context.command

        return 1
    }
}