package com.github.quiltservertools.ledger.webui.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LedgerPlayer
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.future.future
import java.util.*

private const val LOGIN_FAILED = 401
private const val LOGOUT = 200

class AuthManager : AccessManager {

    private val users = mutableListOf<LedgerPlayer>()

    override fun manage(handler: Handler, ctx: Context, routeRoles: MutableSet<RouteRole>) {
        handler.handle(ctx.future(
            Ledger.future {
                if (hasRole(ctx, routeRoles)) {
                    handler.handle(ctx)
                } else if (ctx.sessionAttribute<UUID>("uuid") != null) {
                    failLogin(ctx, "You do not have the perms to access this page", false)
                } else {
                    ctx.status(LOGIN_FAILED).result("You must log in before accessing the Ledger panel").redirect("http://localhost:8080/login")
                }
            }
        ))
    }

    private fun hasRole(ctx: Context, routeRoles: MutableSet<RouteRole>): Boolean {
        // Not logged in
        ctx.sessionAttribute<UUID>("uuid")?: run {
            // If route roles contains NO_AUTH, we allow, otherwise deny
            return routeRoles.any { (it as WebUiRoles).level == WebUiRoles.NO_AUTH.level }
        }

        // Grab UUID from session storage
        val uuid: UUID? = ctx.sessionAttribute("uuid")

        // Check if there is a match between the role level of the user and the role level required
        val playerResult = users.find { it.uuid == uuid }
        for (role in WebUiRoles.values()) {
            if (routeRoles.any { routeRole -> (routeRole as WebUiRoles).level <= (playerResult?.webUiPerms ?: 0) }) {
                return true
            }
        }

        return false
    }

    fun logIn(ctx: Context) {
        ctx.future(
            Ledger.future {
                val username = ctx.formParam("username") ?: return@future failLogin(ctx, "User not supplied", true)
                val password = ctx.formParam("userPassword")?: return@future failLogin(ctx, "Password not supplied", true)
                val playerResult = DatabaseManager.searchPlayer(username)

                if (BCrypt.verifyer().verify(password.toCharArray(), playerResult.passwordHash?: "").verified && playerResult.webUiPerms > 0) {
                    users.add(playerResult)
                    ctx.sessionAttribute("uuid", playerResult.uuid)
                    ctx.redirect("/")
                    return@future
                } else if (playerResult.webUiPerms == 0.toByte()) {
                    failLogin(ctx, "You do not have the permissions to log in", false)
                }

                failLogin(ctx, "Incorrect password or username", true)
            }
        )
    }

    fun logOut(ctx: Context) {
        val uuid = ctx.consumeSessionAttribute<UUID>("uuid")
        users.removeIf { it.uuid == uuid }
        ctx.status(LOGOUT).redirect("/login?logout")
    }

    suspend fun updatePlayer(uuid: UUID) {
        users.removeIf { it.uuid == uuid }
        val player = DatabaseManager.searchPlayer(uuid)
        users.add(player)
    }

    private fun failLogin(ctx: Context, message: String, hasPerms: Boolean) {
        if (hasPerms) {
            ctx.status(LOGIN_FAILED).result(message).redirect("/login?password")
        } else {
            ctx.status(LOGIN_FAILED).result(message).redirect("/login?auth")
        }
    }
}
