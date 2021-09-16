package com.github.quiltservertools.ledger.utility

data class Negatable<T> (val property: T, val allowed: Boolean) {
    companion object {
        fun <U> allow(value: U) = Negatable(value, true)
        fun <U> deny(value: U) = Negatable(value, false)
    }
}
