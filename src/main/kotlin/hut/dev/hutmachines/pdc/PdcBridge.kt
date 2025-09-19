package hut.dev.hutmachines.pdc

import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class PdcBridge(plugin: Plugin) {
    private val kState    = NamespacedKey(plugin, "state")
    private val kProgress = NamespacedKey(plugin, "progress")
    private val kCache    = NamespacedKey(plugin, "slotcache")

    fun writeState(block: Block, state: String, progress: Int?) {
        val ts = block.state as? TileState ?: return
        val pdc = ts.persistentDataContainer
        pdc.set(kState, PersistentDataType.STRING, state)
        if (progress != null) pdc.set(kProgress, PersistentDataType.INTEGER, progress)
        ts.update(true, false)
    }

    fun readState(block: Block): Pair<String?, Int?> {
        val ts = block.state as? TileState ?: return null to null
        val pdc = ts.persistentDataContainer
        return pdc.get(kState, PersistentDataType.STRING) to pdc.get(kProgress, PersistentDataType.INTEGER)
    }

    fun writeSmallSlotCache(block: Block, json: String) {
        val ts = block.state as? TileState ?: return
        ts.persistentDataContainer.set(kCache, PersistentDataType.STRING, json)
        ts.update(true, false)
    }

    fun readSmallSlotCache(block: Block): String? {
        val ts = block.state as? TileState ?: return null
        return ts.persistentDataContainer.get(kCache, PersistentDataType.STRING)
    }
}