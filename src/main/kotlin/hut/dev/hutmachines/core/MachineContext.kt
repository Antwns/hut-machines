package hut.dev.hutmachines.core

import hut.dev.hutmachines.db.DbSlot
import hut.dev.hutmachines.model.MachineSpec
import hut.dev.hutmachines.model.RecipeSpec
import org.bukkit.World
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Runtime context for a single placed machine block.
 * We’ll attach inventories/energy holders here in the TE step.
 */
class MachineContext(
    val plugin: Plugin,
    val machineId: String,
    val spec: MachineSpec,
    val recipes: List<RecipeSpec>,
    val world: World,
    val x: Int,
    val y: Int,
    val z: Int,
    val owner: UUID?
) {
    val state = MachineState()

    /**
     * Minimal, engine-agnostic slot data that we can persist.
     * Feed this from your actual inventory handling code.
     */
    data class SlotData(
        val itemNbtJson: String?,  // your serialized ItemStack (JSON/NBT), or null if empty
        val amount: Int?,          // stack size (nullable = unknown/empty)
        val durability: Int?       // optional durability metadata
    )

    /** Sparse slot storage: only store indices that currently hold something meaningful. */
    private val slots: ConcurrentHashMap<Int, SlotData> = ConcurrentHashMap()

    fun blockKey(): BlockKey = BlockKey(world.name, x, y, z)

    // -------------------------
    // Slot helpers (simple API)
    // -------------------------

    /** Replace/insert a slot snapshot. Call this whenever a slot changes. */
    fun setSlot(index: Int, itemNbtJson: String?, amount: Int?, durability: Int?) {
        if (itemNbtJson == null && amount == null && durability == null) {
            // treat as empty
            slots.remove(index)
        } else {
            slots[index] = SlotData(itemNbtJson, amount, durability)
        }
    }

    /** Explicitly clear a slot. */
    fun clearSlot(index: Int) {
        slots.remove(index)
    }

    /** Read-only view (e.g., for GUIs or debugging). */
    fun getSlot(index: Int): SlotData? = slots[index]

    /** Current populated slot indices (not necessarily contiguous). */
    fun populatedSlotIndices(): Set<Int> = slots.keys

    // -------------------------------------------
    // Persistence: build a pure DbSlot snapshot
    // -------------------------------------------

    /**
     * Convert current in-memory slots to DbSlot list (pure data, safe for async DB writes).
     * We deliberately set a placeholder UUID here; the registry worker resolves the real machineId
     * by block position and normalizes the list before writing to DB.
     */
    fun toDbSlots(): List<DbSlot> {
        // Placeholder; will be replaced by MachineInstanceRegistryWorker normalization.
        val ZERO_UUID = UUID(0L, 0L)

        // Sort indices for stable ordering (not required, just neat for debugging).
        val indices = slots.keys.toList().sorted()

        return indices.map { idx ->
            val s = slots[idx]!!
            DbSlot(
                machineId = ZERO_UUID,
                slotIndex = idx,
                itemNbt = s.itemNbtJson,
                amount = s.amount,
                durability = s.durability
            )
        }
    }

    // Placeholders for next steps (TE will wire these):
    // lateinit var inputInv: ???   // stored inventory for inputs
    // lateinit var outputInv: ???  // stored inventory for outputs
    // var energy: ??? = null       // stored energy holder (if enabled)
}