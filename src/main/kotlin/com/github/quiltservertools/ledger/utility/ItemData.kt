package com.github.quiltservertools.ledger.utility

import net.minecraft.core.component.DataComponentPatch
import net.minecraft.world.item.Item

data class ItemData(val item: Item, val changes: DataComponentPatch)
