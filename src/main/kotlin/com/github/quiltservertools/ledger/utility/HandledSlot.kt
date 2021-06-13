package com.github.quiltservertools.ledger.utility

import net.minecraft.screen.ScreenHandler

interface HandledSlot {
    fun getHandler(): ScreenHandler
    fun setHandler(handler: ScreenHandler)
}
