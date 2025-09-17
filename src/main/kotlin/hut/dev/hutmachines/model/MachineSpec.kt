package hut.dev.hutmachines.model

data class MachineSpec(
    val id: String,
    val model: String,
    val inventory: InventorySpec = InventorySpec(),
    val slots: SlotsSpec = SlotsSpec(),
    val energy: EnergySpec = EnergySpec(),
    val processing: ProcessingSpec = ProcessingSpec(),
    val behavior: BehaviorSpec = BehaviorSpec(),
    val gui: GuiSpec = GuiSpec()
)
