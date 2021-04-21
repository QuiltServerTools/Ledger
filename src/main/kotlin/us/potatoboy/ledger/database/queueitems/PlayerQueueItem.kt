package us.potatoboy.ledger.database.queueitems

import us.potatoboy.ledger.database.DatabaseManager
import java.util.*

class PlayerQueueItem(val uuid: UUID, val name: String) : QueueItem {
    override fun insert() {
        DatabaseManager.addPlayer(uuid, name)
    }
}