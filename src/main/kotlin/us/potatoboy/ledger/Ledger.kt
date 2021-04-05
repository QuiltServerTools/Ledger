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
import us.potatoboy.ledger.actionutils.ActionSearchParams
import us.potatoboy.ledger.actionutils.Preview
import us.potatoboy.ledger.commands.LedgerCommand
import us.potatoboy.ledger.database.DatabaseManager
import us.potatoboy.ledger.database.QueueDrainer
import us.potatoboy.ledger.listeners.BlockEventListener
import us.potatoboy.ledger.listeners.PlayerEventListener
import us.potatoboy.ledger.registry.ActionRegistry
import us.potatoboy.ledger.utility.Dispatcher
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object Ledger : DedicatedServerModInitializer {
    val modId = "ledger"
    val logger = LogManager.getLogger(modId)
    val server: MinecraftServer by lazy { FabricLoader.getInstance().gameInstance as MinecraftServer }
    const val PAGESIZE = 8 //TODO make configurable

    val searchCache = ConcurrentHashMap<String, ActionSearchParams>();
    val previewCache = ConcurrentHashMap<UUID, Preview>();

    private var queueDrainerJob: Job? = null

    override fun onInitializeServer() {
        val version = FabricLoader.getInstance().getModContainer(modId).get().metadata.version
        logger.info("Initializing Ledger ${version.friendlyString}")

        DatabaseManager.ensureTables()
        ActionRegistry.registerDefaultTypes()
        initListeners()
        ServerLifecycleEvents.SERVER_STARTING.register(::serverStarting)
        ServerLifecycleEvents.SERVER_STARTED.register(::serverStarted)
        ServerLifecycleEvents.SERVER_STOPPED.register(::serverStopped)
        CommandRegistrationCallback.EVENT.register(::commandRegistration)
    }

    private fun serverStarting(server: MinecraftServer) {
        val idSet = setOf<Identifier>()
            .plus(Registry.BLOCK.ids)
            .plus(Registry.ITEM.ids)
            .plus(Registry.ENTITY_TYPE.ids)

        queueDrainerJob = GlobalScope.launch(Dispatchers.IO) {
            server.saveProperties.generatorOptions.dimensions.ids.forEach { DatabaseManager.insertWorld(it) }

            logger.info("Inserting ${idSet.size} registry keys into database...")
            //idSet.forEach { DatabaseManager.insertObject(it) }
            logger.info("Registry insert complete. Starting queue drainer")

            QueueDrainer.run()
        }
    }

    private fun serverStarted(server: MinecraftServer) {
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
    }

    private fun commandRegistration(dispatcher: Dispatcher, dedicated: Boolean) {
        LedgerCommand(dispatcher).register()
    }
}

