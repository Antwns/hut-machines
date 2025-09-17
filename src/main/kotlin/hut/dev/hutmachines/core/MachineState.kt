package hut.dev.hutmachines.core
/**
 * Minimal state we’ll persist in the tile-entity’s NBT.
 * - autoEnabled: ON/OFF toggle for auto-processing
 * - activeRecipeIndex: which recipe is running (index into ConfigWorker.getRecipesFor(id)), or null
 * - progress: ticks progressed in the active recipe
 */
data class MachineState(
    var running: Boolean = false,
    var ticksLeft: Int = 0,
    var currentRecipeId: String? = null,
    var energyBuffer: Int = 0,
    var autoEnabled: Boolean = true
)