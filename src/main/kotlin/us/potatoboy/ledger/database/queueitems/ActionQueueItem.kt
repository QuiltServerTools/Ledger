package us.potatoboy.ledger.database.queueitems

import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.database.queueitems.QueueItem

class ActionQueueItem(val action: ActionType) : QueueItem {
    override fun insert() {
        DatabaseManager.insertAction(action)
    }
}