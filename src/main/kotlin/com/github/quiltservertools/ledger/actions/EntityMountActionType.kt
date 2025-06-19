package com.github.quiltservertools.ledger.actions

class EntityMountActionType : AbstractActionType() {
    override val identifier = "entity-mount"

    override fun getTranslationType() = "entity"
}
