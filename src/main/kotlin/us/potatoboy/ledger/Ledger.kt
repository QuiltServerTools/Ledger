package us.potatoboy.ledger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.Preview
import us.potatoboy.ledger.commands.registerCommands
import us.potatoboy.ledger.config.CONFIG_PATH
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.listeners.registerBlockListeners
import us.potatoboy.ledger.listeners.registerEntityListeners
import us.potatoboy.ledger.listeners.registerPlayerListeners
import us.potatoboy.ledger.network.registerNetworking
import us.potatoboy.ledger.registry.ActionRegistry
import us.potatoboy.ledger.utility.Dispatcher
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object Ledger : DedicatedServerModInitializer, CoroutineScope {
    const val MOD_ID = "ledger"

    val logger: Logger = LogManager.getLogger("Ledger")
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
        DatabaseManager.setValues(server.getSavePath(WorldSavePath.ROOT).resolve("ledger.sqlite").toFile())
        DatabaseManager.ensureTables()
        ActionRegistry.registerDefaultTypes()
        initListeners()
        registerNetworking()

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
    }

    private fun serverStopped(server: MinecraftServer) {
        runBlocking {
            // TODO make actions SharedFlow fully drain somehow
        }
    }

    private fun initListeners() {
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


