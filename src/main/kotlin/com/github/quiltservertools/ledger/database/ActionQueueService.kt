package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.utility.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue

object ActionQueueService {
    private val queue = LinkedBlockingQueue<ActionType>()
    private lateinit var job: Job

    val size: Int get() = queue.size

    fun start() {
        job = Ledger.launch {
            prepareNextBatch()
        }
    }

    fun addToQueue(action: ActionType): Boolean {
        if (action.isBlacklisted()) return false

        return queue.add(action)
    }

    suspend fun drainAll() {
        job.cancel()
        while (queue.isNotEmpty()) {
            drainBatch()
        }
    }

    private suspend fun drainBatch() {
        val batch = mutableListOf<ActionType>()
        queue.drainTo(batch, Ledger.config[DatabaseSpec.batchSize])

        DatabaseManager.logActionBatch(batch)
    }

    private suspend fun prepareNextBatch() {
        job = Ledger.launch {
            if (queue.size < Ledger.config[DatabaseSpec.batchSize]) {
                delay(Ledger.config[DatabaseSpec.batchDelay].ticks)
            }
            drainBatch()
            prepareNextBatch()
        }
    }
}
