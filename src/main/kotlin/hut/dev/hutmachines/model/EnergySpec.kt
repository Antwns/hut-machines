package hut.dev.hutmachines.model

data class EnergySpec(
    val enabled: Boolean = false,
    val perTick: Long = 0, // negative = consume, positive = produce
    val buffer: Long = 1000
)
