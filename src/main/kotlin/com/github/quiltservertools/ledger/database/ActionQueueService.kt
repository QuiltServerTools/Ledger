package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.utility.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

object ActionQueueService {
    // Reoptimization: shutdown drain uses larger chunks - there is no tick
    // pressure while stopping, so fewer transactions finish the drain faster.
    private const val SHUTDOWN_BATCH_MULTIPLIER = 4
    private const val MIN_SHUTDOWN_BATCH_SIZE = 5000

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
        // Reoptimization: at shutdown there is no tick pressure - flush in larger
        // chunks (4x the runtime batch size, at least 5000) so the queue drains
        // with fewer transactions and the server stops waiting less time.
        val shutdownBatchSize = maxOf(
            Ledger.config[DatabaseSpec.batchSize] * SHUTDOWN_BATCH_MULTIPLIER,
            MIN_SHUTDOWN_BATCH_SIZE,
        )
        while (queue.isNotEmpty()) {
            drainBatch(shutdownBatchSize)
        }
    }

    private suspend fun drainBatch(size: Int = Ledger.config[DatabaseSpec.batchSize]) {
        val batch = mutableListOf<ActionType>()
        queue.drainTo(batch, size)

        // Reoptimization: NonCancellable so a shutdown-time cancellation can never
        // abort a half-written batch. The rows were already removed from the queue,
        // so a cancelled write would silently drop logged actions.
        withContext(NonCancellable) {
            DatabaseManager.logActionBatch(batch)
        }
    }

    private suspend fun prepareNextBatch() {
        job = Ledger.launch {
            if (queue.size < Ledger.config[DatabaseSpec.batchSize]) {
                delay(Ledger.config[DatabaseSpec.batchDelay].ticks)
            }
            if (queue.isNotEmpty()) drainBatch()
            prepareNextBatch()
        }
    }
}
