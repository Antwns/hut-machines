package hut.dev.hutmachines

import hut.dev.hutmachines.commands.HmCommand
import org.bukkit.plugin.java.JavaPlugin

class HutMachinesPlugin : JavaPlugin() {
    override fun onEnable() {
        getCommand("hm")?.apply {
            setExecutor(HmCommand)
            tabCompleter = HmCommand
        } ?: logger.warning("Command 'hm' not found in plugin.yml")
    }
}