package com.github.quiltservertools.ledger.api

import java.nio.file.Path
import javax.sql.DataSource

interface DatabaseExtension : LedgerExtension {
    fun getDataSource(savePath: Path): DataSource
}
