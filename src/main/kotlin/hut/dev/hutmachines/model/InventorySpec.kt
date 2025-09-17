package hut.dev.hutmachines.model

data class InventorySpec(
    val type: InventoryKind = InventoryKind.CHEST,
    val size: Int = 27 // only used for CHEST; must be 9,18,27,36,45,54
)
