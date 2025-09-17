package hut.dev.hutmachines.model

data class GuiToggleSpec(
    val enabled: Boolean = true,
    val slot: Int = 4,
    val onItem: String = "minecraft:lime_dye",
    val offItem: String = "minecraft:red_dye",
    val onLabel: String = "Auto: ON",
    val offLabel: String = "Auto: OFF"
)
