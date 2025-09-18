package hut.dev.hutmachines.workers

import hut.dev.hutmachines.model.MachineSpec
import org.bukkit.plugin.Plugin

/**
 * Registry skeleton for machines loaded by ConfigWorker.
 * Step 2: collects/validates & logs.
 * Step 3: will register Nova blocks/items/tile entities.
 */
object MachineSpecRegistry {

    data class RegisteredMachine(
        val id: String,
        val spec: MachineSpec
    )

    private val _machines = linkedMapOf<String, RegisteredMachine>()
    val machines: Map<String, RegisteredMachine> get() = _machines

    fun registerAll(owner: Plugin) {
        _machines.clear()

        val loaded = ConfigWorker.machines
        if (loaded.isEmpty()) {
            owner.logger.warning("[MachineRegistry] No machines to register (ConfigWorker.machines is empty)")
            return
        }

        for ((id, spec) in loaded) {
            if (_machines.containsKey(id)) {
                owner.logger.warning("[MachineRegistry] Duplicate machine id '$id' ignored")
                continue
            }
            _machines[id] = RegisteredMachine(id, spec)
        }

        owner.logger.info(
            "[MachineRegistry] Prepared ${_machines.size} machine registrations: " +
                    _machines.keys.joinToString(", ")
        )
    }

    fun get(id: String): RegisteredMachine? = _machines[id]
}