package com.github.quiltservertools.ledger.utility

data class Negatable<T> (val property: T, val allowed: Boolean) {
    companion object {
        @JvmStatic
        fun <U> allow(value: U) = Negatable(value, true)
        @JvmStatic
        fun <U> deny(value: U) = Negatable(value, false)
    }
}
