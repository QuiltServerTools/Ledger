package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.SearchResults
import com.github.quiltservertools.ledger.database.DatabaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.enums.ChestType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockBox
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.UUID

private val inspectingUsers = HashSet<UUID>()

fun PlayerEntity.isInspecting() = inspectingUsers.contains(this.uuid)

fun PlayerEntity.inspectOn(): Int {
    inspectingUsers.add(this.uuid)
    this.sendMessage(
        Text.translatable(
            "text.ledger.inspect.toggle",
            "text.ledger.inspect.on".translate().formatted(Formatting.GREEN)
        ).setStyle(TextColorPallet.secondary),
        false
    )

    return 1
}

fun PlayerEntity.inspectOff(): Int {
    inspectingUsers.remove(this.uuid)
    this.sendMessage(
        Text.translatable(
            "text.ledger.inspect.toggle",
            "text.ledger.inspect.off".translate().formatted(Formatting.RED)
        ).setStyle(TextColorPallet.secondary),
        false
    )

    return 1
}

fun ServerCommandSource.inspectBlock(pos: BlockPos) {
    val source = this

    Ledger.launch(Dispatchers.IO) {
        var area = BlockBox(pos)

        val state = source.world.getBlockState(pos)
        if (state.isOf(Blocks.CHEST)) {
            getOtherChestSide(state, pos)?.let {
                area = BlockBox.create(pos, it)
            }
        }

        val params = ActionSearchParams.build {
            bounds = area
            worlds = mutableSetOf(Negatable.allow(source.world.registryKey.value))
        }

        Ledger.searchCache[source.name] = params

        MessageUtils.warnBusy(source)
        val results = DatabaseManager.searchActions(params, 1)

        if (results.actions.isEmpty()) {
            source.sendError(Text.translatable("error.ledger.command.no_results"))
            return@launch
        }

        MessageUtils.sendSearchResults(
            source,
            results,
            Text.translatable(
                "text.ledger.header.search.pos",
                "${pos.x} ${pos.y} ${pos.z}".literal()
            ).setStyle(TextColorPallet.primary)
        )
    }
}

private fun getOtherChestSide(state: BlockState, pos: BlockPos): BlockPos? {
    val type = state.get(ChestBlock.CHEST_TYPE)
    return if (type != ChestType.SINGLE) {
        // We now need to query other container results in the same chest
        val facing = state.get(ChestBlock.FACING)
        if (type == ChestType.RIGHT) {
            // Chest is right, so left as you look at it
            pos.offset(facing.rotateCounterclockwise(Direction.Axis.Y))
        } else {
            pos.offset(facing.rotateClockwise(Direction.Axis.Y))
        }
    } else null
}

suspend fun PlayerEntity.getInspectResults(pos: BlockPos): SearchResults {
    val source = this.commandSource
    val params = ActionSearchParams.build {
        bounds = BlockBox(pos)
    }

    Ledger.searchCache[source.name] = params
    MessageUtils.warnBusy(source)
    return DatabaseManager.searchActions(params, 1)
}

