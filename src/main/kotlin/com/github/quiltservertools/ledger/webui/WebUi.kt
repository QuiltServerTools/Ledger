package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.Ledger
import com.github.quiltservertools.ledger.config.WebuiSpec
import com.github.quiltservertools.ledger.config.config
import com.github.quiltservertools.ledger.webui.auth.AuthManager
import com.github.quiltservertools.ledger.webui.auth.WebUiRoles
import io.javalin.Javalin
import io.javalin.http.HttpCode
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.command.ServerCommandSource
import java.time.format.DateTimeFormatter
import java.util.*

object WebUi {

    private const val vueDir = "/vue"
    lateinit var commandSource: ServerCommandSource

    val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss")

    private val authManager = AuthManager()

    private var app: Javalin = Javalin.create {
        it.addStaticFiles(vueDir, Location.CLASSPATH)
        it.enableWebjars()
        it.showJavalinBanner = false
        it.accessManager(authManager)
    }

    init {
        val port = config[WebuiSpec.port]
        Ledger.logger.info("Loading WebUI on port $port")
        JavalinVue.rootDirectory {
            it.explicitPath(FabricLoader.getInstance().getModContainer(Ledger.MOD_ID).get().getPath("$vueDir"))
        }

        app.get("/", VueComponent("dashboard"), WebUiRoles.READ)
        app.get("/search", VueComponent("search"), WebUiRoles.READ)
        app.get("/search/results", VueComponent("search_results"), WebUiRoles.READ)
        app.get("/login", VueComponent("login"), WebUiRoles.NO_AUTH)
        app.get("/logout", authManager::logOut)
        app.get("/account", VueComponent("account"), WebUiRoles.READ)
        app.get("/inspect", VueComponent("inspect"), WebUiRoles.READ)
        app.get("/inspect/results", VueComponent("inspect_results"), WebUiRoles.READ)
        app.get("/users", VueComponent("user_manager"), WebUiRoles.ADMIN)

        app.error(HttpCode.NOT_FOUND.status, VueComponent("404"))

        // API listeners
        app.get("/api/overview", Handlers::handleOverview, WebUiRoles.READ)
        app.get("/api/players_overview", Handlers::handlePlayersOverview, WebUiRoles.READ)
        app.get("/api/searchinit", Handlers::searchInit, WebUiRoles.READ)
        app.get("/api/search", Handlers::search, WebUiRoles.READ)
        app.get("/api/inspectinit", Handlers::inspectInit, WebUiRoles.READ)
        app.get("/api/inspect", Handlers::inspect, WebUiRoles.READ)
        app.post("/api/login", authManager::logIn, WebUiRoles.NO_AUTH)
        app.get("/api/pfp", Handlers::showPfp, WebUiRoles.READ)
        app.get("/api/account", Handlers::account, WebUiRoles.READ)
        app.get("/api/users", Handlers::getUserManager, WebUiRoles.ADMIN)
        app.get("/api/updateuser", Handlers::updateUser, WebUiRoles.ADMIN)
        app.start(port)
    }

    fun shutdown() {
        app.stop()
    }

    suspend fun reloadUser(uuid: UUID) {
        authManager.updatePlayer(uuid)
    }
}
