package us.potatoboy.ledger.database

import us.potatoboy.ledger.actions.ActionType
import java.util.concurrent.LinkedBlockingQueue

object ActionQueue {
    private val queue = LinkedBlockingQueue<ActionType>()

    fun addActionToQueue(actionType: ActionType) {
        queue.add(actionType)
    }

    fun getQueue() = queue

    fun size() = queue.size
}