package com.github.quiltservertools.ledger.utility

/**
 * Set while an explosion lets a block react to being hit (wind charges toggling doors, levers and
 * the like). ServerExplosionMixin logs the resulting state change itself, so block hooks that are
 * also reached on that path check this to avoid logging it a second time.
 */
object ExplosionHitTracker {
    @JvmStatic
    var active = false
}
