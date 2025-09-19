// src/main/kotlin/hut/dev/hutmachines/workers/MachineInstanceRegistryWorker.kt
package hut.dev.hutmachines.workers

import hut.dev.hutmachines.HutMachinesPlugin
import hut.dev.hutmachines.core.BlockKey
import hut.dev.hutmachines.core.MachineContext
import hut.dev.hutmachines.db.DbMachine
import hut.dev.hutmachines.db.DbSlot
import hut.dev.hutmachines.db.DatabaseWorker
import hut.dev.hutmachines.model.MachineSpec
import hut.dev.hutmachines.model.RecipeSpec
import org.bukkit.World
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MachineInstanceRegistryWorker {

    private val _instances = ConcurrentHashMap<BlockKey, MachineContext>()
    val instances: Map<BlockKey, MachineContext> get() = _instances

    /**
     * Create a runtime instance, register it in memory, and UPSERT it to DB.
     * - If a DB row already exists at (world,x,y,z), we reuse its UUID.
     * - Otherwise we generate a new UUID and insert it.
     */
    fun createInstance(
        plugin: Plugin,
        machineId: String,
        spec: MachineSpec,
        recipes: List<RecipeSpec>,
        world: World,
        x: Int, y: Int, z: Int,
        owner: UUID?
    ): MachineContext {
        val ctx = MachineContext(plugin, machineId, spec, recipes, world, x, y, z, owner)
        _instances[ctx.blockKey()] = ctx

        // --- DB: upsert machine row ---
        val worldName = world.name
        val existing = DatabaseWorker.INSTANCE.findMachineIdAt(worldName, x, y, z)
        val id = if (existing != null) existing else UUID.randomUUID()

        val db = DbMachine(
            id = id,
            typeId = machineId,                // map your spec id if you prefer a different string
            world = worldName,
            x = x, y = y, z = z,
            autoProcess = false     // you defined autoProcess in your spec conventions
        )
        DatabaseWorker.INSTANCE.upsertMachine(db)

        return ctx
    }

    fun get(worldName: String, x: Int, y: Int, z: Int): MachineContext? =
        _instances[BlockKey(worldName, x, y, z)]

    /**
     * Remove a runtime instance from memory AND delete the DB row at that block position.
     */
    fun remove(worldName: String, x: Int, y: Int, z: Int): MachineContext? {
        val removed = _instances.remove(BlockKey(worldName, x, y, z))
        DatabaseWorker.INSTANCE.deleteMachineAt(worldName, x, y, z)
        return removed
    }

    // -----------------------------
    // Slot change → database helpers
    // -----------------------------

    /**
     * Debounced slot write:
     * - Resolves the machine UUID by (world,x,y,z). If not found, returns silently.
     * - Rewrites the provided slots with the resolved machineId (s.copy(machineId = id)).
     * - Defers DB write via DebouncedWriter to avoid spam.
     *
     * Call this AFTER you’ve updated your runtime inventory.
     */
    fun onSlotsChangedDebounced(
        plugin: Plugin,
        world: World,
        x: Int, y: Int, z: Int,
        slots: List<DbSlot>
    ) {
        val id = DatabaseWorker.INSTANCE.findMachineIdAt(world.name, x, y, z)
        if (id == null) {
            // No persisted machine at this block yet; nothing to write.
            return
        }

        // Ensure slots carry the correct machineId
        val normalized = slots.map { s -> if (s.machineId == id) s else s.copy(machineId = id) }

        val hm = plugin as? HutMachinesPlugin
        if (hm == null) {
            // Plugin type not as expected; write immediately to avoid losing data.
            DatabaseWorker.INSTANCE.replaceSlots(id, normalized)
            return
        }

        hm.debouncedWriter.requestFlush(id) {
            DatabaseWorker.INSTANCE.replaceSlots(id, normalized)
        }
    }

    /**
     * Immediate slot flush (no debounce). Useful on shutdown or when you need
     * strong durability guarantees right away.
     */
    fun flushSlotsNow(
        plugin: Plugin,
        world: World,
        x: Int, y: Int, z: Int,
        slots: List<DbSlot>
    ) {
        val id = DatabaseWorker.INSTANCE.findMachineIdAt(world.name, x, y, z)
        if (id == null) return

        val normalized = slots.map { s -> if (s.machineId == id) s else s.copy(machineId = id) }

        val hm = plugin as? HutMachinesPlugin
        if (hm != null) {
            hm.debouncedWriter.flushNow(id) {
                DatabaseWorker.INSTANCE.replaceSlots(id, normalized)
            }
        } else {
            DatabaseWorker.INSTANCE.replaceSlots(id, normalized)
        }
    }
}