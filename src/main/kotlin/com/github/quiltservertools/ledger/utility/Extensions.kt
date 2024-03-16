package com.github.quiltservertools.ledger.utility

import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

fun MutableText.appendWithSpace(text: Text) {
    this.append(text)
    this.append(" ".literal())
}

fun String.literal() = Text.literal(this)
fun String.translate() = Text.translatable(this)

fun ServerCommandSource.hasPlayer() = this.entity is ServerPlayerEntity

// fun String.translate(vararg args: Any) = TranslatableText(this, args)

fun MinecraftServer.getWorld(identifier: Identifier?) = getWorld(RegistryKey.of(RegistryKeys.WORLD, identifier))

val TICK_LENGTH = 50.milliseconds
inline val Int.ticks get() = this * TICK_LENGTH
