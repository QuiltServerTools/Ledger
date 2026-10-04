package com.github.quiltservertools.ledger.config

import com.fasterxml.jackson.annotation.JsonProperty
import com.github.quiltservertools.ledger.actions.ActionType
import kotlin.math.abs

data class CombinationBlacklistRule(
    val type: String? = null,
    val world: String? = null,
    @param:JsonProperty("object") val objectId: String? = null,
    val source: String? = null,
    val centerX: Int? = null,
    val centerY: Int? = null,
    val centerZ: Int? = null,
    val range: Int? = null,
) {
    fun matches(action: ActionType): Boolean {
        // partial area spec (missing any of the 4 fields, or range < 1) is treated as no area filter
        val hasAreaFilter = centerX != null && centerY != null && centerZ != null && range != null && range >= 1
        if (type == null && world == null && objectId == null && source == null && !hasAreaFilter) return false
        if (type != null && type != action.identifier) return false
        if (world != null && world != action.world?.toString()) return false
        if (objectId != null &&
            objectId != action.objectIdentifier.toString() &&
            objectId != action.oldObjectIdentifier.toString()
        ) {
            return false
        }
        if (source != null) {
            val sourceMatch = source == action.sourceName ||
                source == "@${action.sourceProfile?.name}"
            if (!sourceMatch) return false
        }
        if (hasAreaFilter) {
            val r = range!!
            val pos = action.pos
            if (abs(pos.x - centerX!!) > r || abs(pos.y - centerY!!) > r || abs(pos.z - centerZ!!) > r) return false
        }
        return true
    }
}
