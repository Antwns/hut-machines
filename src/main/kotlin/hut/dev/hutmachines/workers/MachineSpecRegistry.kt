package hut.dev.hutmachines.workers
import hut.dev.hutmachines.model.MachineSpec
import xyz.xenondevs.nova.addon.Addon

/**
 * Registry skeleton for machines loaded by ConfigWorker.
 *
 * Step 2: only collects/validates & logs.
 * Step 3: we’ll extend this to register Nova blocks + items + tile entities.
 */
internal object MachineSpecRegistry {

    /** Simple holder for later Nova objects (will be filled in Step 3). */
    internal data class RegisteredMachine(
        val id: String,
        val spec: MachineSpec
        // later:
        // val blockType: ???,
        // val itemType: ???,
        // val tileEntityType: ???
    )

    private val _machines = linkedMapOf<String, RegisteredMachine>()
    val machines: Map<String, RegisteredMachine> get() = _machines

    fun registerAll(addon: Addon) {
        _machines.clear()

        val loaded = ConfigWorker.machines
        if (loaded.isEmpty()) {
            addon.logger.warn("[MachineRegistry] No machines to register (ConfigWorker.machines is empty)")
            return
        }

        for ((id, spec) in loaded) {
            if (_machines.containsKey(id)) {
                addon.logger.warn("[MachineRegistry] Duplicate machine id '$id' ignored")
                continue
            }
            _machines[id] = RegisteredMachine(id, spec)
        }

        addon.logger.info("[MachineRegistry] Prepared ${_machines.size} machine registrations: ${_machines.keys.joinToString(", ")}")
    }
}
