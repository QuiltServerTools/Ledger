package com.github.quiltservertools.ledger.utility

import net.minecraft.world.entity.player.Player

interface PlayerCausable {
    val causingPlayer: Player?
}
