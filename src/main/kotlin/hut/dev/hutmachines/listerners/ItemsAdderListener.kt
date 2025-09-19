package hut.dev.hutmachines.listeners

import hut.dev.hutmachines.db.DatabaseWorker
import hut.dev.hutmachines.db.DbMachine
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.UUID
import org.bukkit.plugin.Plugin

class ItemsAdderListener(private val plugin: Plugin) : Listener {

    @EventHandler
    fun onIACustomBlockPlace(e: dev.lone.itemsadder.api.Events.CustomBlockPlaceEvent) {
        val block = e.block
        val iaId = e.namespacedID ?: return

        val id = UUID.randomUUID()
        val m = DbMachine(
            id = id,
            typeId = iaId, // later: map IA id -> your HutMachines spec id
            world = block.world.name,
            x = block.x, y = block.y, z = block.z,
            autoProcess = false
        )
        DatabaseWorker.INSTANCE.upsertMachine(m)
        plugin.logger.info("[HutMachines][IA] Placed $iaId @ ${block.world.name} ${block.x},${block.y},${block.z} (id=$id)")
    }

    @EventHandler
    fun onIACustomBlockBreak(e: dev.lone.itemsadder.api.Events.CustomBlockBreakEvent) {
        val block = e.block
        val deleted = DatabaseWorker.INSTANCE.deleteMachineAt(block.world.name, block.x, block.y, block.z)
        if (deleted) {
            plugin.logger.info("[HutMachines][IA] Removed machine @ ${block.world.name} ${block.x},${block.y},${block.z}")
        }
    }
}