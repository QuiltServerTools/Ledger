package com.github.quiltservertools.ledger.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

fun interface PlayerBlockAttackCallback {
    fun attack(
        player: PlayerEntity,
        world: World,
        pos: BlockPos,
        direction: Direction,
        hand: Hand
    ): ActionResult

    companion object {
        val EVENT: Event<PlayerBlockAttackCallback> =
            EventFactory.createArrayBacked(PlayerBlockAttackCallback::class.java) { listeners ->
                PlayerBlockAttackCallback { player, world, pos, direction, hand ->
                    for (listener in listeners) {
                        val result = listener.attack(player, world, pos, direction, hand)

                        if (result != ActionResult.PASS) {
                            return@PlayerBlockAttackCallback result
                        }
                    }

                    return@PlayerBlockAttackCallback ActionResult.PASS
                }
            }
    }
}
