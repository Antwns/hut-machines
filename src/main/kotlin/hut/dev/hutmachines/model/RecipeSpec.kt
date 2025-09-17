package hut.dev.hutmachines.model

data class RecipeSpec(
    val name: String? = null,
    val inputs: Map<Int, EncodedItem> = emptyMap(),    // slot -> item
    val outputs: Map<Int, EncodedItem> = emptyMap(),   // slot -> item
    val duration: Int? = null,                          // ticks; falls back to machine.processing.intervalTicks
    val energyPerTick: Long? = null,                    // optional override
    val durability: Map<Int, Int> = emptyMap()          // slot -> damage per completion
)
