package com.github.quiltservertools.ledger.actions

import com.mojang.authlib.GameProfile
import net.minecraft.block.BlockState
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import com.github.quiltservertools.ledger.config.ActionsSpec
import com.github.quiltservertools.ledger.config.config
import java.time.Instant

interface ActionType {
    val identifier: String
    var timestamp: Instant
    var pos: BlockPos
    var world: Identifier?
    var objectIdentifier: Identifier
    var oldObjectIdentifier: Identifier
    var blockState: BlockState?
    var oldBlockState: BlockState?
    var sourceName: String
    var sourceProfile: GameProfile?
    var extraData: String?
    var rolledBack: Boolean

    fun rollback(world: ServerWorld): Boolean
    fun restore(world: ServerWorld): Boolean
    fun previewRollback(world: ServerWorld, player: ServerPlayerEntity)
    fun previewRestore(world: ServerWorld, player: ServerPlayerEntity)
    fun getTranslationType(): String
    fun getMessage(): Text

    fun isBlacklisted() = config[ActionsSpec.typeBlacklist].contains(identifier) ||
            config[ActionsSpec.objectBlacklist].contains(objectIdentifier) ||
            config[ActionsSpec.objectBlacklist].contains(oldObjectIdentifier) ||
            config[ActionsSpec.sourceBlacklist].contains(sourceName) ||
            config[ActionsSpec.worldBlacklist].contains(world)
}
