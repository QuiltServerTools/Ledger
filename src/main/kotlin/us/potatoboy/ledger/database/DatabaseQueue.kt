package us.potatoboy.ledger.database

import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.database.queueitems.QueueItem
import java.util.concurrent.LinkedBlockingQueue

object DatabaseQueue {
    private val queue = LinkedBlockingQueue<QueueItem>()

    fun addActionToQueue(queueItem: QueueItem) {
        queue.add(queueItem)
    }

    fun getQueue() = queue

    fun size() = queue.size
}