package com.github.quiltservertools.ledger.api

import net.minecraft.util.Identifier

interface LedgerExtension {
    fun getIdentifier(): Identifier

    fun events(events: ExtensionEvents)
}
