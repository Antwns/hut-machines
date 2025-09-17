package hut.dev.hutmachines.model

data class SlotsSpec(
    val input: List<Int> = listOf(6, 7),
    val output: List<Int> = listOf(8, 9),
    val fuel: List<Int> = emptyList()
)
