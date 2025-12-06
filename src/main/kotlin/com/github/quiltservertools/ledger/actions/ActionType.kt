package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.config.ActionsSpec
import com.github.quiltservertools.ledger.config.config
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.NameAndId
import java.time.Instant
import kotlin.time.ExperimentalTime

interface ActionType {
    var id: Int
    val identifier: String
    var timestamp: Instant
    var pos: BlockPos
    var world: ResourceLocation?
    var objectIdentifier: ResourceLocation
    var oldObjectIdentifier: ResourceLocation
    var objectState: String?
    var oldObjectState: String?
    var sourceName: String
    var sourceProfile: NameAndId?
    var extraData: String?
    var rolledBack: Boolean

    fun rollback(server: MinecraftServer): Boolean
    fun restore(server: MinecraftServer): Boolean
    fun previewRollback(preview: Preview, player: ServerPlayer)
    fun previewRestore(preview: Preview, player: ServerPlayer)
    fun getTranslationType(): String

    @ExperimentalTime
    fun getMessage(source: CommandSourceStack): Component

    fun isBlacklisted() = config[ActionsSpec.typeBlacklist].contains(identifier) ||
            config[ActionsSpec.objectBlacklist].contains(objectIdentifier) ||
            config[ActionsSpec.objectBlacklist].contains(oldObjectIdentifier) ||
            config[ActionsSpec.sourceBlacklist].contains(sourceName) ||
            config[ActionsSpec.sourceBlacklist].contains("@${sourceProfile?.name}") ||
            config[ActionsSpec.worldBlacklist].contains(world)
}
