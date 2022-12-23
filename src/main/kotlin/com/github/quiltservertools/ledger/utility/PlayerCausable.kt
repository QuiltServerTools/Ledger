package com.github.quiltservertools.ledger.utility

import net.minecraft.entity.player.PlayerEntity

interface PlayerCausable {
    val causingPlayer: PlayerEntity?
}
