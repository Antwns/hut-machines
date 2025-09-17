package hut.dev.hutmachines.model

data class BehaviorSpec(
    val onOutputFull: OutputPolicy = OutputPolicy.BLOCK,
    val pauseIfNoEnergy: Boolean = true
)
