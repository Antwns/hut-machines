package hut.dev.hutmachines

import hut.dev.hutmachines.commands.buildHmRoot
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class HutMachinesPlugin : JavaPlugin() {
    override fun onEnable() {
        // Register Brigadier commands on the COMMANDS lifecycle event
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event: ReloadableRegistrarEvent<Commands> ->
            event.registrar().register(buildHmRoot())
        }
        logger.info("Registered Paper Brigadier command: /hm delete")
    }
}
