package com.github.quiltservertools.ledger.export

import com.github.quiltservertools.ledger.actions.ActionType
import java.nio.file.Path

abstract class AbstractExportAdapter {
    abstract suspend fun exportFromData(actions: List<ActionType>, exportDir: Path): Path?
}
