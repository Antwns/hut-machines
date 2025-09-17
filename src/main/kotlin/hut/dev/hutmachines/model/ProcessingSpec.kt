package hut.dev.hutmachines.model

data class ProcessingSpec(
    val auto: Boolean = true,           // default state on new placements
    val intervalTicks: Int = 200,       // also used as default recipe duration if not set
    val toggleable: Boolean = true
)
