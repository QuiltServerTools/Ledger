package com.github.quiltservertools.ledger.permissions

import com.github.quiltservertools.ledger.config.SearchSpec
import com.github.quiltservertools.ledger.config.config
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates
import net.minecraft.commands.CommandSourceStack
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.PermissionLevel
import java.util.function.Predicate

private const val NAMESPACE = "ledger"
val DEFAULT_PERMISSION_LEVEL = PermissionLevel.ADMINS

enum class Permissions(
    private val identifier: Identifier,
    private val fallback: PermissionLevel = DEFAULT_PERMISSION_LEVEL,
) {
    ROOT(commandIdentifier("root")),
    INSPECT(commandIdentifier("inspect")),
    NETWORKING(Identifier.fromNamespaceAndPath(NAMESPACE, "networking")),
    PLAYER(commandIdentifier("player")),
    PREVIEW(commandIdentifier("preview")),
    PURGE(commandIdentifier("purge"), PermissionLevel.byId(config[SearchSpec.purgePermissionLevel])),
    ROLLBACK(commandIdentifier("rollback")),
    RESTORE(commandIdentifier("restore")),
    SEARCH(commandIdentifier("search")),
    STATUS(commandIdentifier("status")),
    TP(commandIdentifier("tp")),
    ;

    companion object {
        fun has(perm: Permissions): Predicate<CommandSourceStack> =
            PermissionPredicates.require(perm.identifier, perm.fallback)

        fun check(player: ServerPlayer, perm: Permissions): Boolean =
            player.permissionContext.checkPermission(perm.identifier).get()
    }
}

private fun commandIdentifier(name: String) = Identifier.fromNamespaceAndPath(NAMESPACE, "command/$name")
