package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.actions.ActionType
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SentAction(
    val identifier: String,
    var time: String,
    var x: Int,
    var y: Int,
    var z: Int,
    var world: String,
    var objectString: String,
    var oldObjectString: String,
    var sourceName: String,
    var sourceProfile: String
) {
    companion object {
        fun fromActionType(action: ActionType): SentAction {
            val objectIdentifier = action.objectIdentifier.toString()
            val oldObjectIdentifier = action.objectIdentifier.toString()
            val time = action.timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss"))
            val pos = action.pos
            val world = action.world.toString()?: ""
            val identifier = action.identifier
            val sourceName = action.sourceName
            val sourceProfile = action.sourceProfile?.name?: ""
            return SentAction(identifier, time, pos.x, pos.y, pos.z, world, objectIdentifier, oldObjectIdentifier, sourceName, sourceProfile)
        }
    }
}

data class Length(val pages: Int)
