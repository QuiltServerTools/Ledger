package us.potatoboy.ledger.actions

import com.mojang.authlib.GameProfile
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.item.Item
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.time.Instant
import java.util.*

interface ActionType {
    val identifier: String
    var timestamp: Instant
    var pos: BlockPos
    var world: Identifier?
    var objectIdentifier: Identifier
    var blockState: BlockState?
    var sourceName: String
    var sourceProfile: GameProfile?
    var extraData: String?

    fun rollback(): Boolean
    fun getTranslationType(): String
    fun getMessage(): Text
}