package com.github.quiltservertools.ledger.api

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.config.config
import java.util.*

object ExtensionManager {
    private val extensions = mutableListOf<LedgerExtension>()

    private var databaseExtension: Optional<DatabaseExtension> = Optional.empty()

    val commands = mutableListOf<CommandExtension>()

    fun registerExtension(extension: LedgerExtension) {
        extensions.add(extension)

        if (extension is DatabaseExtension) {
            if(databaseExtension.isEmpty) {
                databaseExtension = Optional.of(extension)
            } else {
                failExtensionRegistration(extension)
            }
        }

        if (extension is CommandExtension) {
            commands.add(extension)
        }

        extension.getConfigSpecs().forEach {
            config.addSpec(it)
        }
    }

    private fun failExtensionRegistration(extension: LedgerExtension) {
        Ledger.logger.error("Unable to load extension ${extension.getIdentifier()}")
    }

    fun getDatabaseExtensionOptional() = databaseExtension
}
