package com.github.quiltservertools.ledger.database

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.actions.ActionType
import com.github.quiltservertools.ledger.actions.ItemChangeActionType
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.utility.ticks
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.util.math.BlockPos
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
        val containerMap = hashMapOf<BlockPos, MutableList<ItemChangeActionType>>()
        batch.removeIf {
            if (it !is ItemChangeActionType) return@removeIf false
            val list = containerMap.computeIfAbsent(it.pos) { mutableListOf() }
            val action = list.firstOrNull { other ->
                other.itemData == it.itemData && other.objectIdentifier == it.objectIdentifier &&
                        it.sourceName == other.sourceName && it.sourceProfile == other.sourceProfile
            }
            if (action != null) {
                action.count += it.count
                true
            } else {
                list.add(it)
                false
            }
        }
        batch.removeIf { it is ItemChangeActionType && it.count == 0 } // remove empty events

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
