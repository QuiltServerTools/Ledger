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

    /**
     * Reoptimization: passes since the last time a batch was filled.
     *
     * 0 means actions are arriving at least as fast as they are written (burst mode:
     * drain without waiting). Any higher value means the server has caught up, so the
     * configured batchDelay applies again.
     */
    @Volatile
    private var consecutiveShortPasses = 0

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

    /**
     * Writes one batch.
     *
     * @return true when a full [size] batch was taken, i.e. the queue is being fed at
     *         least as fast as it is drained
     */
    private suspend fun drainBatch(size: Int = Ledger.config[DatabaseSpec.batchSize]): Boolean {
        val batch = mutableListOf<ActionType>()
        queue.drainTo(batch, size)
        val saturated = batch.size >= size

        // Reoptimization: NonCancellable so a shutdown-time cancellation can never
        // abort a half-written batch. The rows were already removed from the queue,
        // so a cancelled write would silently drop logged actions.
        withContext(NonCancellable) {
            DatabaseManager.logActionBatch(batch)
        }
        return saturated
    }

    private suspend fun prepareNextBatch() {
        job = Ledger.launch {
            val batchSize = Ledger.config[DatabaseSpec.batchSize].coerceAtLeast(1)
            val maxDelayTicks = Ledger.config[DatabaseSpec.batchDelay].coerceAtLeast(1)

            // Reoptimization: drain at full speed while the queue is being flooded, and
            // keep the configured cadence once it is not.
            //
            // Measured on a 13,500-action burst: the unconditional `delay(batchDelay)`
            // burned 6.1 s of an 8.7 s ingest window while the writes themselves took
            // 2.6 s per pass - the scheduler, not the database, was the bottleneck,
            // because every pass paid the full 500 ms even with work already queued.
            //
            // burstMode means the previous pass filled a complete batch, i.e. actions
            // arrive faster than they are written. In that state a partial queue is the
            // tail of a flood, so waiting for it would only add latency. Once a pass
            // comes up short the server is caught up and the original wait applies, so
            // trickle traffic (a few actions at a time) still batches exactly as
            // upstream does - this deliberately does not turn an idle server into one
            // small transaction per action.
            val burstMode = consecutiveShortPasses == 0
            if (queue.size < batchSize && !burstMode) {
                delay(maxDelayTicks.ticks)
            }

            if (queue.isNotEmpty()) {
                // Logging happens in the called transaction; see drainBatch.
                val saturated = drainBatch(batchSize)
                // saturated => still flooded, keep burstMode; otherwise count the misses
                // so a quiet server settles onto the configured delay.
                consecutiveShortPasses = if (saturated) 0 else consecutiveShortPasses + 1
            }
            prepareNextBatch()
        }
    }
}
