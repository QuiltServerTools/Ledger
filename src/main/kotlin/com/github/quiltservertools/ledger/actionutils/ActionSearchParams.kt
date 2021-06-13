package com.github.quiltservertools.ledger.actionutils

import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.time.Duration

class ActionSearchParams(
    val min: BlockPos?,
    val max: BlockPos?,
    val time: Duration?,
    val actions: Set<String>?,
    val objects: Set<Identifier>?,
    val sourceNames: Set<String>?,
    val sourcePlayerNames: Set<String>?,
    val worlds: Set<Identifier>?,
) {
    private constructor(builder: Builder) : this(
        builder.min,
        builder.max,
        builder.time,
        builder.actions,
        builder.objects,
        builder.sourceNames,
        builder.sourcePlayerNames,
        builder.worlds
    )

    fun isEmpty() = listOf(min, max, time, actions, objects, sourceNames, sourcePlayerNames, worlds).all { it == null }

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var min: BlockPos? = null
        var max: BlockPos? = null
        var time: Duration? = null
        var actions: MutableSet<String>? = null
        var objects: MutableSet<Identifier>? = null
        var sourceNames: MutableSet<String>? = null
        var sourcePlayerNames: MutableSet<String>? = null
        var worlds: MutableSet<Identifier>? = null

        fun build() = ActionSearchParams(this)
    }
}
