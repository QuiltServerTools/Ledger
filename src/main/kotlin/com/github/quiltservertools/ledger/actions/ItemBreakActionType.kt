package com.github.quiltservertools.ledger.actions

// Rolling back drops the broken item where it broke, restoring removes it again
class ItemBreakActionType : ItemPickUpActionType() {
    override val identifier = "item-break"
}
