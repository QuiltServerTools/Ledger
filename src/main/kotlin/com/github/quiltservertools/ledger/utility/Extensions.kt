package com.github.quiltservertools.ledger.utility

import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

fun MutableComponent.appendWithSpace(text: Component) {
    this.append(text)
    this.append(" ".literal())
}

fun String.literal() = Component.literal(this)
fun String.translate() = Component.translatable(this)

fun CommandSourceStack.hasPlayer() = this.entity is ServerPlayer

// fun String.translate(vararg args: Any) = TranslatableText(this, args)

fun MinecraftServer.getWorld(identifier: ResourceLocation?) = getLevel(
    ResourceKey.create(Registries.DIMENSION, identifier)
)

val TICK_LENGTH = 50.milliseconds
inline val Int.ticks get() = this * TICK_LENGTH
