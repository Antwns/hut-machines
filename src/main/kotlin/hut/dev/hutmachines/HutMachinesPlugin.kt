// src/main/kotlin/hut/dev/hutmachines/HutMachinesPlugin.kt
package hut.dev.hutmachines

import hut.dev.hutmachines.commands.buildHmRoot
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class HutMachinesPlugin : JavaPlugin() {
    override fun onEnable() {
        // Load configs + register specs
        ConfigWorker.loadAll(this)
        MachineSpecRegistry.registerAll(this)

        // Register Brigadier command with Paper lifecycle
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val node = buildHmRoot(this)   // <-- pass plugin
            logger.info("[HutMachines] About to register /${node.literal} with children: ${node.children.map { it.name }}")
            // simplest: node + a description string
            event.registrar().register(node, "HutMachines root command")

        }

        logger.info("Registered Paper Brigadier command: /hm (mark, delete)")
    }
}
