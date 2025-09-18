package hut.dev.hutmachines

import hut.dev.hutmachines.commands.buildHmRoot
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class HutMachinesPlugin : JavaPlugin() {
    override fun onEnable() {
        ConfigWorker.loadAll(this)
        MachineSpecRegistry.registerAll(this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(buildHmRoot(this))   // <-- pass 'this'
        }
        logger.info("Registered Paper Brigadier command: /hm delete")
    }
}