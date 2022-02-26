package com.github.quiltservertools.ledger.utility

import com.github.quiltservertools.ledger.Ledger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import net.minecraft.server.world.ServerWorld
import net.minecraft.world.World
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

object McDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        context[McExecutor]?.executor?.invoke(block)
            ?: error(IllegalStateException("No McExecutor in $context context"))
    }
}

class McExecutor(val executor: (Runnable) -> Unit) : AbstractCoroutineContextElement(McExecutor) {
    companion object Key : CoroutineContext.Key<McExecutor>
}

fun launchMain(executor: (Runnable) -> Unit, block: suspend CoroutineScope.() -> Unit): Job =
    Ledger.launch(McDispatcher + McExecutor(executor), block = block)

fun World.launchMain(block: suspend CoroutineScope.() -> Unit) {
    when (this) {
        is ServerWorld -> launchMain(server::execute, block)
    }
}
