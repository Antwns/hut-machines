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
        val itemNbtJson: String?,  // serialized ItemStack (JSON/NBT), or null if empty
        val amount: Int?,          // stack size
        val durability: Int?       // optional durability metadata
    )

    /** Sparse slot storage: only indices with meaningful content. */
    private val slots: ConcurrentHashMap<Int, SlotData> = ConcurrentHashMap()

    /** Dirty tracking for debounced DB flushes. */
    @Volatile var dirty: Boolean = false
        private set

    fun blockKey(): BlockKey = BlockKey(world.name, x, y, z)

    /** Replace/insert a slot snapshot. Mark dirty when changed. */
    fun setSlot(index: Int, itemNbtJson: String?, amount: Int?, durability: Int?) {
        if (itemNbtJson == null && amount == null && durability == null) {
            if (slots.remove(index) != null) dirty = true
        } else {
            val prev = slots[index]
            val next = SlotData(itemNbtJson, amount, durability)
            if (prev != next) {
                slots[index] = next
                dirty = true
            }
        }
    }

    /** Explicitly clear a slot. */
    fun clearSlot(index: Int) {
        if (slots.remove(index) != null) dirty = true
    }

    fun getSlot(index: Int): SlotData? = slots[index]
    fun populatedSlotIndices(): Set<Int> = slots.keys

    /**
     * Convert current in-memory slots to DbSlot list (pure data, safe for async DB writes).
     * Machine id will be normalized to the real UUID by the registry before writing to DB.
     */
    fun toDbSlots(): List<DbSlot> {
        val ZERO_UUID = UUID(0L, 0L)
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

    /** Mark clean after a successful DB write. */
    fun clearDirty() { dirty = false }

    // Placeholders for next steps (TE will wire these):
    // lateinit var inputInv: ???   // stored inventory for inputs
    // lateinit var outputInv: ???  // stored inventory for outputs
    // var energy: ??? = null       // stored energy holder (if enabled)
}