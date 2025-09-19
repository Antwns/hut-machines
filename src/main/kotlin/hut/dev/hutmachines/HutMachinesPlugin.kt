// src/main/kotlin/hut/dev/hutmachines/HutMachinesPlugin.kt
package hut.dev.hutmachines

import hut.dev.hutmachines.commands.buildHmRoot
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import hut.dev.hutmachines.db.DatabaseWorker
import hut.dev.hutmachines.db.DebouncedWriter
import hut.dev.hutmachines.pdc.PdcBridge
import hut.dev.hutmachines.listeners.ItemsAdderListener
import java.nio.file.Paths

class HutMachinesPlugin : JavaPlugin() {

    var SyncToken = 517 //Use THIS to sync.

    lateinit var pdc: PdcBridge
    lateinit var debouncedWriter: DebouncedWriter

    override fun onEnable() {
        // Load configs + register specs
        ConfigWorker.loadAll(this)
        MachineSpecRegistry.registerAll(this)

        // Register Brigadier command with Paper lifecycle
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val node = buildHmRoot(this)   // <-- pass plugin
            logger.info("[HutMachines] About to register /${node.literal} with children: ${node.children.map { it.name }}")
            event.registrar().register(node, "HutMachines root command")
        }
        logger.info("Registered Paper Brigadier command: /hm (mark, info, delete)")

        // Ensure data folder exists (first run)
        if (!dataFolder.exists()) dataFolder.mkdirs()

        // DB init
        val dbFile = Paths.get(dataFolder.toString(), "hutmachines-db")
        DatabaseWorker.INSTANCE.apply {
            printStartupStats = false
            init(dbFile)
        }
        logger.info("HutMachines DB ready; ${DatabaseWorker.INSTANCE.getMachineCount()} machines indexed.")

        // Fast per-block hot state + debounced DB writes
        pdc = PdcBridge(this)
        debouncedWriter = DebouncedWriter(this, delayTicks = 10L)

        // ItemsAdder listener (IA is present on your test server)
        val ia = server.pluginManager.getPlugin("ItemsAdder")
        if (ia != null && ia.isEnabled) {
            server.pluginManager.registerEvents(ItemsAdderListener(this), this)
            logger.info("ItemsAdder detected; IA listeners registered.")
        } else {
            logger.info("ItemsAdder not found or not enabled; IA listeners skipped.")
        }
    }

    override fun onDisable() {
        // If/when you track dirty machines:
        // debouncedWriter.flushAll { id ->
        //     val inst = MachineInstanceRegistryWorker.instance[id] ?: return@flushAll
        //     val snapshot = buildDbSlotsFromRuntime(inst) // pure data, no Bukkit objects
        //     DatabaseWorker.INSTANCE.replaceSlots(id, snapshot)
        // }
    }
}