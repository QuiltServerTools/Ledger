package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.utility.Negatable
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import java.time.Instant

data class ActionSearchParams(
    val bounds: BlockBox?,
    val before: Instant?,
    val after: Instant?,
    var actions: MutableSet<Negatable<String>>?,
    var objects: MutableSet<Negatable<Identifier>>?,
    var sourceNames: MutableSet<Negatable<String>>?,
    var sourcePlayerNames: MutableSet<Negatable<String>>?,
    var worlds: MutableSet<Negatable<Identifier>>?,
) {
    private constructor(builder: Builder) : this(
        builder.bounds,
        builder.before,
        builder.after,
        builder.actions,
        builder.objects,
        builder.sourceNames,
        builder.sourcePlayerNames,
        builder.worlds
    )

    fun isEmpty() = listOf(bounds, before, after, actions, objects, sourceNames, sourcePlayerNames, worlds).all { it == null }

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var bounds: BlockBox? = null
        var before: Instant? = null
        var after: Instant? = null
        var actions: MutableSet<Negatable<String>>? = null
        var objects: MutableSet<Negatable<Identifier>>? = null
        var sourceNames: MutableSet<Negatable<String>>? = null
        var sourcePlayerNames: MutableSet<Negatable<String>>? = null
        var worlds: MutableSet<Negatable<Identifier>>? = null

        fun build() = ActionSearchParams(this)
    }
}
