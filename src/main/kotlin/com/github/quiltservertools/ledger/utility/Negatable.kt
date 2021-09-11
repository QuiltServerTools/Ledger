package com.github.quiltservertools.ledger.utility

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType

data class Negatable<T> (val property: T, val allowed: Boolean) {
    companion object {
        fun <U> allow(value: U) = Negatable(value, true)
        fun <U> deny(value: U) = Negatable(value, false)
        fun <U> getNegatable(stringReader: StringReader, argument: ArgumentType<U>): Negatable<U> {
            if (stringReader.string.isEmpty()) return allow(argument.parse(stringReader))
            if (stringReader.string[stringReader.cursor] == '!') {
                stringReader.skip()
                return deny(argument.parse(stringReader))
            }
            return allow(argument.parse(stringReader))
        }
    }
}
