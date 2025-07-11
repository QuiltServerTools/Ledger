package com.github.quiltservertools.ledger.registry

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.*
import com.github.quiltservertools.ledger.database.DatabaseManager
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import kotlinx.coroutines.launch
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
        registerActionType { BlockPlaceActionType() }
        registerActionType { BlockChangeActionType() }
        registerActionType { ItemInsertActionType() }
        registerActionType { ItemRemoveActionType() }
        registerActionType { ItemPickUpActionType() }
        registerActionType { ItemDropActionType() }
        registerActionType { EntityKillActionType() }
        registerActionType { EntityChangeActionType() }
        registerActionType { EntityMountActionType() }
        registerActionType { EntityDismountActionType() }
    }

    fun getType(id: String) = actionTypes[id]

    fun getTypes(): ObjectSet<String> = actionTypes.keys
}
