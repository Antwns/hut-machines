package hut.dev.hutmachines.listeners

import hut.dev.hutmachines.workers.MachineInstanceRegistryWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import hut.dev.hutmachines.model.MachineSpec
import hut.dev.hutmachines.model.RecipeSpec
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class ItemsAdderListener(private val plugin: Plugin) : Listener {

    @EventHandler
    fun onIACustomBlockPlace(e: dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent) {
        val block = e.block
        val iaId = e.namespacedID ?: return

        // Resolve spec from registry; if missing, ignore placement
        val reg = MachineSpecRegistry.get(iaId) ?: return
        val spec: MachineSpec = reg.spec
        val recipes: List<RecipeSpec> = emptyList() // hook up when you have per-machine recipes

        MachineInstanceRegistryWorker.createInstance(
            plugin = plugin,
            machineId = iaId,
            spec = spec,
            recipes = recipes,
            world = block.world,
            x = block.x, y = block.y, z = block.z,
            owner = e.player?.uniqueId
        )
        plugin.logger.info("[HutMachines][IA] Placed $iaId @ ${block.world.name} ${block.x},${block.y},${block.z}")
    }

    @EventHandler
    fun onIACustomBlockBreak(e: dev.lone.itemsadder.api.Events.CustomBlockBreakEvent) {
        val block = e.block
        MachineInstanceRegistryWorker.remove(block.world.name, block.x, block.y, block.z)
        plugin.logger.info("[HutMachines][IA] Removed machine @ ${block.world.name} ${block.x},${block.y},${block.z}")
    }
}