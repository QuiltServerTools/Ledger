package us.potatoboy.ledger

import kotlinx.coroutines.*
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.Preview
import us.potatoboy.ledger.commands.LedgerCommand
import us.potatoboy.ledger.config.CONFIG_PATH
import us.potatoboy.ledger.config.config
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.database.DatabaseQueue
import us.potatoboy.ledger.database.QueueDrainer
import us.potatoboy.ledger.database.queueitems.RegistryQueueItem
import us.potatoboy.ledger.listeners.BlockEventListener
import us.potatoboy.ledger.listeners.EntityCallbackListener
import us.potatoboy.ledger.listeners.PlayerEventListener
import us.potatoboy.ledger.network.Networking
import us.potatoboy.ledger.registry.ActionRegistry
import us.potatoboy.ledger.utility.Dispatcher
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

object Ledger : DedicatedServerModInitializer, CoroutineScope {

    const val MOD_ID = "ledger"

    // I hate detekt
    // :detekt failed
    // MagicNumber
    const val PERMISSION_LEVEL = 3

    val logger: Logger = LogManager.getLogger("Ledger")
    val server: MinecraftServer by lazy { FabricLoader.getInstance().gameInstance as MinecraftServer }
    val searchCache = ConcurrentHashMap<String, ActionSearchParams>()
    val previewCache = ConcurrentHashMap<UUID, Preview>()

    private var queueDrainerJob: Job? = null

    override val coroutineContext: CoroutineContext = Dispatchers.IO

    override fun onInitializeServer() {
        val version = FabricLoader.getInstance().getModContainer(MOD_ID).get().metadata.version
        logger.info("Initializing Ledger ${version.friendlyString}")

        if (!Files.exists(FabricLoader.getInstance().configDir.resolve(CONFIG_PATH))) {
            logger.info("No config file, Creating")
            Files.copy(
                FabricLoader.getInstance().getModContainer(MOD_ID).get().getPath(CONFIG_PATH),
                FabricLoader.getInstance().configDir.resolve(CONFIG_PATH)
            )
        }
        config.validateRequired()

        DatabaseManager.ensureTables()
        ActionRegistry.registerDefaultTypes()
        initListeners()
        ServerLifecycleEvents.SERVER_STARTING.register(::serverStarting)
        ServerLifecycleEvents.SERVER_STOPPED.register(::serverStopped)
        CommandRegistrationCallback.EVENT.register(::commandRegistration)
        Networking
    }

    private fun serverStarting(server: MinecraftServer) {
        val idSet = setOf<Identifier>()
            .plus(Registry.BLOCK.ids)
            .plus(Registry.ITEM.ids)
            .plus(Registry.ENTITY_TYPE.ids)

        queueDrainerJob = Ledger.launch(Dispatchers.IO) {
            server.saveProperties.generatorOptions.dimensions.ids.forEach { DatabaseManager.insertWorld(it) }

            logger.info("Inserting ${idSet.size} registry keys into the database queue...")
            idSet.forEach { DatabaseQueue.addActionToQueue(RegistryQueueItem(it)) }

            QueueDrainer.run()
        }
    }

    private fun serverStopped(server: MinecraftServer) {
        runBlocking {
            QueueDrainer.stop()
            queueDrainerJob?.join()
        }
    }

    private fun initListeners() {
        PlayerEventListener
        BlockEventListener
        EntityCallbackListener
    }

    private fun commandRegistration(dispatcher: Dispatcher, dedicated: Boolean) = LedgerCommand(dispatcher).register()

    fun identifier(path: String) = Identifier(MOD_ID, path)
}
