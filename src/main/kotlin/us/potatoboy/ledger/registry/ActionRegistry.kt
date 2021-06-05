package us.potatoboy.ledger.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import kotlinx.coroutines.launch
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actions.*
import us.potatoboy.ledger.database.DatabaseManager
import java.util.function.Supplier

private const val MAX_LENGTH = 16

object ActionRegistry {
    private val actionTypes = Object2ObjectOpenHashMap<String, Supplier<ActionType>>()

    // TODO make this better
    // TODO create some sort of action identifier with grouping
    fun registerActionType(supplier: Supplier<ActionType>) {
        val id = supplier.get().identifier
        require(id.length <= MAX_LENGTH)

        actionTypes.putIfAbsent(id, supplier)
        Ledger.launch {
            DatabaseManager.registerActionType(id)
        }
    }

    fun registerDefaultTypes() {
        registerActionType { BlockBreakActionType() }
        registerActionType { BlockChangeActionType("block-place") }
        registerActionType { ItemInsertActionType() }
        registerActionType { ItemRemoveActionType() }
        registerActionType { EntityKillActionType() }
    }

    fun getType(id: String) = actionTypes[id]

    fun getTypes(): ObjectSet<String> = actionTypes.keys
}
