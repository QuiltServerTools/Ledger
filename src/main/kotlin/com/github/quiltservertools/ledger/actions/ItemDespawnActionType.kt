package com.github.quiltservertools.ledger.actions

import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.utility.Negatable
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component

class ItemDespawnActionType : ItemPickUpActionType() {
    override val identifier = IDENTIFIER

    // The item was logged at the end of its lifetime, so it would despawn again on the next tick
    override fun getRespawnTag(): CompoundTag = super.getRespawnTag().apply { putShort("Age", 0) }

    override fun getRollbackMessage(source: CommandSourceStack): Component? =
        if (canRollback(source)) super.getRollbackMessage(source) else null

    companion object {
        const val IDENTIFIER = "item-despawn"
        const val ROLLBACK_PERMISSION = "ledger.commands.rollback.item-despawn"

        // Stricter than the regular rollback permission (level 3), so despawned items are only brought back on purpose
        private const val ROLLBACK_PERMISSION_LEVEL = 4

        fun canRollback(source: CommandSourceStack): Boolean =
            Permissions.check(source, ROLLBACK_PERMISSION, ROLLBACK_PERMISSION_LEVEL)

        /**
         * Keeps despawned items out of a rollback for sources without [ROLLBACK_PERMISSION]. Asking for them
         * explicitly fails instead, so an empty result is not mistaken for "nothing despawned here".
         */
        fun restrictRollback(source: CommandSourceStack, params: ActionSearchParams) {
            if (canRollback(source)) return

            if (params.actions?.contains(Negatable.allow(IDENTIFIER)) == true) {
                throw SimpleCommandExceptionType(
                    Component.translatable("error.ledger.rollback.item_despawn_permission"),
                ).create()
            }

            params.actions = (params.actions ?: mutableSetOf()).apply { add(Negatable.deny(IDENTIFIER)) }
        }
    }
}
