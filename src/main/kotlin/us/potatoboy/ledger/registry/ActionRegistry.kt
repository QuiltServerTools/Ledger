package us.potatoboy.ledger.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import us.potatoboy.ledger.actions.*
import us.potatoboy.ledger.database.DatabaseManager
import java.util.function.Supplier

object ActionRegistry {
    private val actionTypes = Object2ObjectOpenHashMap<String, Supplier<ActionType>>()

    fun registerActionType(supplier: Supplier<ActionType>) {
        val id = supplier.get().identifier
        if (id.length > 16) throw IllegalArgumentException("ActionId cannot be longer than 16")

        actionTypes.putIfAbsent(id, supplier)
        DatabaseManager.insertActionId(id)
    }

    fun registerDefaultTypes() {
        registerActionType { BlockBreakActionType() }
        registerActionType { BlockChangeActionType("block-place") }
        registerActionType { ItemInsertActionType() }
        registerActionType { ItemRemoveActionType() }
        registerActionType { EntityKillActionType() }
    }

    fun getType(id: String) = actionTypes[id]

    fun getTypes(): ObjectSet<String> {
        return actionTypes.keys
    }
}