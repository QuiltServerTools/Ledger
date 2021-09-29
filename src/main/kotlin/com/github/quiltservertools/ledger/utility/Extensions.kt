package com.github.quiltservertools.ledger.utility

import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.LiteralText
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey

fun MutableText.appendWithSpace(text: Text) {
    this.append(text)
    this.append(" ".literal())
}

fun String.literal() = LiteralText(this)
fun String.translate() = TranslatableText(this)

fun ServerCommandSource.hasPlayer() = this.entity is ServerPlayerEntity

// fun String.translate(vararg args: Any) = TranslatableText(this, args)

fun MinecraftServer.getWorld(identifier: Identifier?) = getWorld(RegistryKey.of(Registry.WORLD_KEY, identifier))
