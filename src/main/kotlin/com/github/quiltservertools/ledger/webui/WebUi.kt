package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.webui.auth.AuthManager
import com.github.quiltservertools.ledger.webui.auth.WebUiRoles
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import net.minecraft.server.command.ServerCommandSource

@SuppressWarnings("MagicNumber")
object WebUi {

    private const val vueDir = "/vue"
    lateinit var commandSource: ServerCommandSource

    private val authManager = AuthManager()

    private var app: Javalin = Javalin.create {
        it.addStaticFiles("$vueDir", Location.CLASSPATH)
        it.enableWebjars()
        it.showJavalinBanner = false
        it.accessManager(authManager)
    }

    init {
        val port = 8080
        Ledger.logger.info("Loading WebUI on port $port")
        JavalinVue.rootDirectory {
            it.classpathPath(vueDir, this.javaClass)
        }
        app.get("/", VueComponent("dashboard"), WebUiRoles.READ)
        app.get("/search", VueComponent("search"), WebUiRoles.READ)
        app.get("/search/results", VueComponent("search_results"), WebUiRoles.READ)
        app.get("/login", VueComponent("login"), WebUiRoles.NO_AUTH)

        app.error(404, "html", VueComponent("404"))

        // API listeners
        app.get("/api/overview", Handlers::handleOverview, WebUiRoles.READ)
        app.get("/api/searchinit", Handlers::searchInit, WebUiRoles.READ)
        app.get("/api/search", Handlers::search, WebUiRoles.READ)
        app.post("/api/login", authManager::logIn, WebUiRoles.NO_AUTH)
        app.get("/logout", authManager::logOut)

        app.start(port)
    }

    fun shutdown() {
        app.stop()
    }
}
