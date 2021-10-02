package com.github.quiltservertools.ledger.webui

import com.github.quiltservertools.ledger.Ledger
import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import net.minecraft.server.command.ServerCommandSource

@SuppressWarnings("MagicNumber")
object WebUi {

    private const val vueDir = "/vue"
    lateinit var commandSource: ServerCommandSource

    private var app: Javalin = Javalin.create {
        it.addStaticFiles("$vueDir", Location.CLASSPATH)
        it.enableWebjars()
        it.showJavalinBanner = false
    }

    init {
        val port = 8080
        Ledger.logger.info("Loading WebUI on port $port")
        JavalinVue.rootDirectory {
            it.classpathPath(vueDir, this.javaClass)
        }
        app.get("/", VueComponent("dashboard"))
        app.error(404, "html", VueComponent("404"))

        // API listeners
        app.get("/api/overview", Handlers::handleOverview)
        app.get("/users", VueComponent("user-overview"))
        app.error(404, "html", VueComponent("not-found"))

        app.get("/api/users", Handlers::handleOverview)

        app.start(port)
    }

    fun shutdown() {
        app.stop()
    }
}
