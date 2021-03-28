package us.potatoboy.ledger.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.actions.BlockBreakActionType
import us.potatoboy.ledger.actions.BlockPlaceActionType
import us.potatoboy.ledger.database.DatabaseManager
import java.util.function.Supplier

object LedgerRegistry {
    private val actionTypes = Object2ObjectOpenHashMap<String, Supplier<ActionType>>()

    fun registerActionType(supplier: Supplier<ActionType>) {
        val id = supplier.get().identifier
        if (id.length > 16) throw IllegalArgumentException("ActionId cannot be longer than 16")

        actionTypes.putIfAbsent(id, supplier)
        DatabaseManager.insertActionId(id)
    }

    fun registerDefaultTypes() {
        registerActionType { BlockBreakActionType() }
        registerActionType { BlockPlaceActionType() }
    }

    fun getType(id: String) = actionTypes[id]
}