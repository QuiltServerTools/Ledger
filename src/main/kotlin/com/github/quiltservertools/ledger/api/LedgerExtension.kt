package com.github.quiltservertools.ledger.api

import com.uchuhimo.konf.ConfigSpec
import net.minecraft.util.Identifier

interface LedgerExtension {
    fun getIdentifier(): Identifier

    // All extension configs should be entirely optional as the default config will not have fallback
    fun getConfigSpecs(): List<ConfigSpec>
}
