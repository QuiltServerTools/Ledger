package us.potatoboy.ledger.database.queueitems

import net.minecraft.util.Identifier
import us.potatoboy.ledger.database.DatabaseManager

class RegistryQueueItem(val identifier: Identifier) : QueueItem {
    override fun insert() {
        DatabaseManager.insertObject(identifier)
    }
}
