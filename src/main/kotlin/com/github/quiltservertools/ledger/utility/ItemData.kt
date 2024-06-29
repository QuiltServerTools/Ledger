package com.github.quiltservertools.ledger.utility

import net.minecraft.component.ComponentChanges
import net.minecraft.item.Item

data class ItemData(val item: Item, val changes: ComponentChanges)
