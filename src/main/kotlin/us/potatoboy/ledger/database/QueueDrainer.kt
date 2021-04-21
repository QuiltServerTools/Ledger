package us.potatoboy.ledger.database

import com.google.common.collect.Queues
import kotlinx.coroutines.runBlocking
import us.potatoboy.ledger.Ledger
import us.potatoboy.ledger.actions.ActionType
import us.potatoboy.ledger.database.queueitems.QueueItem
import java.util.concurrent.TimeUnit

object QueueDrainer : Runnable {
    private var running = false

    override fun run() {
        running = true
        while (running) {
            try {
                val queuedActions = ArrayList<QueueItem>(50)
                Queues.drain(
                    DatabaseQueue.getQueue(),
                    queuedActions,
                    50,
                    5,
                    TimeUnit.SECONDS
                ) //TODO make queue drain size and timeout config

                if (queuedActions.isEmpty()) continue
                runBlocking {
                    DatabaseManager.insertQueued(queuedActions)
                }
            } catch (e: InterruptedException) {
                Ledger.logger.fatal("something bad happened")
                e.printStackTrace()
            }
        }

        forceDrain()
    }

    private fun forceDrain() {
        val queued = mutableListOf<QueueItem>()
        DatabaseQueue.getQueue().drainTo(queued)
        if (queued.isEmpty()) return

        Ledger.logger.info("Draining ${queued.size} remaining actions from action queue. DO NOT KILL. Actions will be lost")
        runBlocking {
            DatabaseManager.insertQueued(queued)
        }
        Ledger.logger.info("Action queue successfully drained")
    }

    fun stop() {
        running = false
    }
}