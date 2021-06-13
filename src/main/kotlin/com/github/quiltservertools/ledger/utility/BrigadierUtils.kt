package com.github.quiltservertools.ledger.utility

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import java.util.*

object BrigadierUtils {
    /**
     * Taken from https://github.com/VelocityPowered/Velocity/blob/8abc9c80a69158ebae0121fda78b55c865c0abad/proxy/src/main/java/com/velocitypowered/proxy/util/BrigadierUtils.java#L38
     *
     * Returns a literal node that redirects its execution to
     * the given destination node.
     *
     * @param alias the command alias
     * @param destination the destination node
     * @return the built node
     */
    fun <S> buildRedirect(alias: String, destination: LiteralCommandNode<S>): LiteralCommandNode<S>? {
        // Redirects only work for nodes with children, but break the top argument-less command.
        // Manually adding the root command after setting the redirect doesn't fix it.
        // See https://github.com/Mojang/brigadier/issues/46). Manually clone the node instead.
        val builder = LiteralArgumentBuilder
            .literal<S>(alias.lowercase(Locale.ENGLISH))
            .requires(destination.requirement)
            .forward(destination.redirect, destination.redirectModifier, destination.isFork)
            .executes(destination.command)
        for (child in destination.children) {
            builder.then(child)
        }
        return builder.build()
    }
}
