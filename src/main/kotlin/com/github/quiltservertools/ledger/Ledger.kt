package com.github.quiltservertools.ledger

import com.github.quiltservertools.ledger.actionutils.ActionSearchParams
import com.github.quiltservertools.ledger.actionutils.Preview
import com.github.quiltservertools.ledger.commands.registerCommands
import com.github.quiltservertools.ledger.config.CONFIG_PATH
import com.github.quiltservertools.ledger.config.DatabaseSpec
import com.github.quiltservertools.ledger.config.WebuiSpec
import com.github.quiltservertools.ledger.database.DatabaseManager
import com.github.quiltservertools.ledger.listeners.registerBlockListeners
import com.github.quiltservertools.ledger.listeners.registerEntityListeners
import com.github.quiltservertools.ledger.listeners.registerPlayerListeners
import com.github.quiltservertools.ledger.listeners.registerWorldEventListeners
import com.github.quiltservertools.ledger.network.Networking
import com.github.quiltservertools.ledger.registry.ActionRegistry
import com.github.quiltservertools.ledger.utility.Dispatcher
import com.github.quiltservertools.ledger.webui.WebUi
import com.uchuhimo.konf.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import com.github.quiltservertools.ledger.config.config as realConfig

object Ledger : DedicatedServerModInitializer, CoroutineScope {
    const val MOD_ID = "ledger"

    const val DEFAULT_DATABASE = "sqlite"
    val logger: Logger = LogManager.getLogger("Ledger")
    val config: Config = realConfig
    lateinit var server: MinecraftServer
    val searchCache = ConcurrentHashMap<String, ActionSearchParams>()
    val previewCache = ConcurrentHashMap<UUID, Preview>()

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override fun onInitializeServer() {
        val version = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version
        logInfo("Initializing Ledger ${version.friendlyString}")

        if (!Files.exists(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH))) {
            logInfo("No config file, Creating")
            Files.copy(
                FabricLoader.getInstance().getModContainer(MOD_ID).get().getPath(CONFIG_PATH),
                FabricLoader.getInstance().configDir.resolve(CONFIG_PATH)
            )
        }
        config.validateRequired()

        ServerLifecycleEvents.SERVER_STARTING.register(::serverStarting)
        ServerLifecycleEvents.SERVER_STOPPED.register(::serverStopped)
        CommandRegistrationCallback.EVENT.register(::commandRegistration)
    }

    private fun serverStarting(server: MinecraftServer) {
        this.server = server
        DatabaseManager.setValues(server.getSavePath(WorldSavePath.ROOT).resolve("ledger.sqlite").toFile(), server)
        DatabaseManager.ensureTables()
        ActionRegistry.registerDefaultTypes()
        initListeners()
        Networking

        val idSet = setOf<Identifier>()
            .plus(Registry.BLOCK.ids)
            .plus(Registry.ITEM.ids)
            .plus(Registry.ENTITY_TYPE.ids)

        Ledger.launch {
            server.saveProperties.generatorOptions.dimensions.ids.forEach { DatabaseManager.registerWorld(it) }

            logInfo("Inserting ${idSet.size} registry keys into the database...")
            DatabaseManager.insertIdentifiers(idSet)
            logInfo("Registry insert complete")
        }

        if (config[WebuiSpec.webui]) {
            WebUi
            WebUi.commandSource = server.commandSource
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun serverStopped(server: MinecraftServer) {
        runBlocking {
            withTimeout(Duration.minutes(config[DatabaseSpec.queueTimeoutMin])) {
                while (DatabaseManager.dbMutex.isLocked) {
                    logInfo("Database queue is still draining. If you exit now actions WILL be lost")
                    delay(Duration.seconds(config[DatabaseSpec.queueCheckDelaySec]))
                }
            }
        }
        if (config[WebuiSpec.webui]) {
            WebUi.shutdown()
        }
    }

    private fun initListeners() {
        registerWorldEventListeners()
        registerPlayerListeners()
        registerBlockListeners()
        registerEntityListeners()
    }

    private fun commandRegistration(dispatcher: Dispatcher, dedicated: Boolean) = registerCommands(dispatcher)

    fun identifier(path: String) = Identifier(MOD_ID, path)
}

fun logDebug(message: String) = Ledger.logger.debug(message)
fun logInfo(message: String) = Ledger.logger.info(message)
fun logWarn(message: String) = Ledger.logger.warn(message)
fun logFatal(message: String) = Ledger.logger.warn(message)
