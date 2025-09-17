package hut.dev.hutmachines.core

import hut.dev.hutmachines.model.MachineSpec
import hut.dev.hutmachines.model.RecipeSpec
import org.bukkit.World
import xyz.xenondevs.nova.addon.Addon
import java.util.UUID
/**
 * Runtime context for a single placed machine block.
 * We’ll attach inventories/energy holders here in the TE step.
 */
class MachineContext(
    val addon: Addon,
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
    // Placeholders for next steps (TE will wire these):
    // lateinit var inputInv: ???   // stored inventory for inputs
    // lateinit var outputInv: ???  // stored inventory for outputs
    // var energy: ??? = null       // stored energy holder (if enabled)
    fun blockKey(): BlockKey = BlockKey(world.name, x, y, z)
}