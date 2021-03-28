package us.potatoboy.ledger.database

import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import us.potatoboy.ledger.actions.ActionType
import java.util.*

data class ActionLookupParams(
    val min: BlockPos?,
    val max: BlockPos?,
    val actions: List<ActionType>?,
    val objects: List<Identifier>?,
    val sourceName: List<String>?,
    val sourceId: List<UUID>?,
    val world: List<Identifier>?
)
