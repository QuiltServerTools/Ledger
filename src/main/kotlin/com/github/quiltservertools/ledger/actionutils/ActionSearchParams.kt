package com.github.quiltservertools.ledger.actionutils

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.utility.Negatable
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockBox
import java.time.Instant
import java.util.*
import kotlin.math.max

data class ActionSearchParams(
    val bounds: BlockBox?,
    val before: Instant?,
    val after: Instant?,
    val rolledBack: Boolean?,
    var actions: MutableSet<Negatable<String>>?,
    var objects: MutableSet<Negatable<Identifier>>?,
    var sourceNames: MutableSet<Negatable<String>>?,
    var sourcePlayerIds: MutableSet<Negatable<UUID>>?,
    var worlds: MutableSet<Negatable<Identifier>>?
) {
    private constructor(builder: Builder) : this(
        builder.bounds,
        builder.before,
        builder.after,
        builder.rolledBack,
        builder.actions,
        builder.objects,
        builder.sourceNames,
        builder.sourcePlayerIds,
        builder.worlds
    )

    fun ensureSpecific() {
        if (bounds == null) {
            throw SimpleCommandExceptionType(Text.translatable("error.ledger.unspecific.range")).create()
        }
        val range = (max(bounds.blockCountX, max(bounds.blockCountY, bounds.blockCountZ)) + 1) / 2
        if (range > Ledger.config[SearchSpec.maxRange]) {
            throw SimpleCommandExceptionType(
                Text.translatable("error.ledger.unspecific.range_to_big", Ledger.config[SearchSpec.maxRange])
            ).create()
        }
        if (sourceNames == null && sourcePlayerIds == null && after == null && before == null) {
            throw SimpleCommandExceptionType(Text.translatable("error.ledger.unspecific.source_or_time")).create()
        }
    }

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var bounds: BlockBox? = null
        var before: Instant? = null
        var after: Instant? = null
        var rolledBack: Boolean? = null
        var actions: MutableSet<Negatable<String>>? = null
        var objects: MutableSet<Negatable<Identifier>>? = null
        var sourceNames: MutableSet<Negatable<String>>? = null
        var sourcePlayerIds: MutableSet<Negatable<UUID>>? = null
        var worlds: MutableSet<Negatable<Identifier>>? = null

        fun build() = ActionSearchParams(this)
    }
}
