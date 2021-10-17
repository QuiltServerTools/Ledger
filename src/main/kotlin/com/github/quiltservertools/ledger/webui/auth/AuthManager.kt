package com.github.quiltservertools.ledger.webui.auth

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.utility.LedgerPlayer
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.http.Context
import io.javalin.http.Handler
import kotlinx.coroutines.future.future
import org.apache.commons.codec.digest.DigestUtils
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
                } else {
                    ctx.status(LOGIN_FAILED).result("You must log in before accessing the Ledger panel").redirect("http://localhost:8080/login")
                }
            }
        ))
    }

    private fun hasRole(ctx: Context, routeRoles: MutableSet<RouteRole>): Boolean {
        // Not logged in
        ctx.sessionAttribute<String>("uuid")?: run {
            // If route roles contains NO_AUTH, we allow, otherwise deny
            return routeRoles.any { (it as WebUiRoles).level == WebUiRoles.NO_AUTH.level }
        }

        // Grab UUID from session storage
        val uuid: UUID = UUID.fromString(ctx.sessionAttribute("uuid"))

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
                val username = ctx.formParam("username") ?: return@future failLogin(ctx, "User not supplied")
                val password = ctx.formParam("userPassword")?: return@future failLogin(ctx, "Password not supplied")
                val playerResult = DatabaseManager.searchPlayer(username)

                if (DigestUtils.sha1Hex(password) == playerResult.passwordHash) {
                    users.add(playerResult)
                    ctx.sessionAttribute("uuid", playerResult.uuid)
                    ctx.redirect("/")
                    return@future
                }

                failLogin(ctx, "Incorrect password or username")
            }
        )
    }

    fun logOut(ctx: Context) {
        val uuid = ctx.consumeSessionAttribute<UUID>("uuid")
        users.removeIf { it.uuid == uuid }
        ctx.status(LOGOUT).redirect("/login?logout")
    }

    private fun failLogin(ctx: Context, message: String) {
        ctx.status(LOGIN_FAILED).result(message).redirect("/login?password")
    }
}
