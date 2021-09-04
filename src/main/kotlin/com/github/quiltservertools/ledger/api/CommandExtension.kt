package com.github.quiltservertools.ledger.api

import com.github.quiltservertools.ledger.commands.BuildableCommand

interface CommandExtension : LedgerExtension {
    fun registerSubcommands(): List<BuildableCommand>
}
