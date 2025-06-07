package com.github.quiltservertools.ledger.actions

class EntityDismountActionType : AbstractActionType() {
    override val identifier = "entity-dismount"

    override fun getTranslationType() = "entity"
}
