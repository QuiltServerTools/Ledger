package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.config.ActionsSpec
import com.github.quiltservertools.ledger.config.config
import com.mojang.authlib.GameProfile
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.time.Instant
import kotlin.time.ExperimentalTime

interface ActionType {
    val identifier: String
    var timestamp: Instant
    var pos: BlockPos
    var world: Identifier?
    var objectIdentifier: Identifier
    var oldObjectIdentifier: Identifier
    var objectState: String?
    var oldObjectState: String?
    var sourceName: String
    var sourceProfile: GameProfile?
    var extraData: String?
    var rolledBack: Boolean

    fun rollback(server: MinecraftServer): Boolean
    fun restore(server: MinecraftServer): Boolean
    fun previewRollback(preview: Preview, player: ServerPlayerEntity)
    fun previewRestore(preview: Preview, player: ServerPlayerEntity)
    fun getTranslationType(): String

    @ExperimentalTime
    fun getMessage(source: ServerCommandSource): Text

    fun isBlacklisted() = config[ActionsSpec.typeBlacklist].contains(identifier) ||
            config[ActionsSpec.objectBlacklist].contains(objectIdentifier) ||
            config[ActionsSpec.objectBlacklist].contains(oldObjectIdentifier) ||
            config[ActionsSpec.sourceBlacklist].contains(sourceName) ||
            config[ActionsSpec.sourceBlacklist].contains("@${sourceProfile?.name}") ||
            config[ActionsSpec.worldBlacklist].contains(world)
}
