package com.github.quiltservertools.ledger.api

import com.github.quiltservertools.ledger.database.DatabaseManager
import org.ktorm.database.Database
import java.nio.file.Path
import javax.sql.DataSource

interface DatabaseExtension : LedgerExtension {
    fun getDataSource(savePath: Path): DataSource

    fun ensureTables(database: Database) {
        DatabaseManager.ensureTables()
    }
}
