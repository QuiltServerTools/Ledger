package us.potatoboy.ledger.actionutils

import net.minecraft.util.math.BlockPos

interface LocationalInventory {
    fun getLocation(): BlockPos
}
