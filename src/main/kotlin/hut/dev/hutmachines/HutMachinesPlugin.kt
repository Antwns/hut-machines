package hut.dev.hutmachines

import hut.dev.hutmachines.commands.buildHmRoot
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class HutMachinesPlugin : JavaPlugin() {
    override fun onEnable() {
        // 1) Load YAMLs and validate
        ConfigWorker.loadAll(this)

        // 2) Prepare machine registrations (just a registry right now)
        MachineSpecRegistry.registerAll(this)

        // 3) Register /hm delete (Brigadier) the Paper way
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(buildHmRoot())
        }
        logger.info("Registered Paper Brigadier command: /hm delete")
    }
}