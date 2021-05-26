package us.potatoboy.ledger.database

import us.potatoboy.ledger.config.ActionsSpec
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.database.queueitems.ActionQueueItem
import us.potatoboy.ledger.database.queueitems.QueueItem
import java.util.concurrent.LinkedBlockingQueue

object DatabaseQueue {
    private val queue = LinkedBlockingQueue<QueueItem>()

    fun addActionToQueue(queueItem: QueueItem) {
        if (queueItem is ActionQueueItem) {
            val action = queueItem.action

            // TODO idk, I don't like this though
            if (
                config[ActionsSpec.typeBlacklist].contains(action.identifier) ||
                config[ActionsSpec.objectBlacklist].contains(action.objectIdentifier) ||
                config[ActionsSpec.objectBlacklist].contains(action.oldObjectIdentifier) ||
                config[ActionsSpec.sourceBlacklist].contains(action.sourceName) ||
                config[ActionsSpec.worldBlacklist].contains(action.world)
            ) return
        }

        queue.add(queueItem)
    }

    fun getQueue() = queue

    fun size() = queue.size
}
