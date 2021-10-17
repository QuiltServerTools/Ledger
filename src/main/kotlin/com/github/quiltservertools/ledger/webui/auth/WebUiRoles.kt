package com.github.quiltservertools.ledger.webui.auth

import io.javalin.core.security.RouteRole

enum class WebUiRoles(val level: Int) : RouteRole {
    READ(1), WRITE(2), NO_AUTH(0), ADMIN(3)
}
