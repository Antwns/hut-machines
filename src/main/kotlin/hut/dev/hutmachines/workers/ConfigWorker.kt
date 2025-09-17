package hut.dev.hutmachines.workers
import hut.dev.hutmachines.model.*

import org.yaml.snakeyaml.Yaml
import xyz.xenondevs.nova.addon.Addon
import java.io.File

// ======== data model (matches our agreed YAML) ===============================











// ======== loader/validator ====================================================

internal object ConfigWorker {

    // Loaded data (read-only from outside)
    private val machinesMutable = linkedMapOf<String, MachineSpec>()
    private val recipesMutable = linkedMapOf<String, List<RecipeSpec>>()

    val machines: Map<String, MachineSpec> get() = machinesMutable
    val recipes: Map<String, List<RecipeSpec>> get() = recipesMutable

    private val yaml = Yaml()
    private val chestSizes = setOf(9, 18, 27, 36, 45, 54)

    fun loadAll(addon: Addon) {
        machinesMutable.clear()
        recipesMutable.clear()

        val base = addon.dataFolder.toFile() // plugins/Nova/addons/hut-machines
        val machinesDir = File(base, "machines").apply { if (!exists()) mkdirs() }
        val recipesDir  = File(base, "recipes").apply { if (!exists()) mkdirs() }

        // load machines
        machinesDir.listFiles { f -> f.isFile && f.name.endsWith(".yml", true) }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                runCatching { loadMachinesFile(file) }
                    .onFailure { addon.logger.warn("[ConfigWorker] Failed to load ${file.name}: ${it.message}") }
            }

        // load recipes (per machine)
        recipesDir.listFiles { f -> f.isFile && f.name.endsWith(".yml", true) }
            ?.sortedBy { it.name }
            ?.forEach { file ->
                runCatching { loadRecipesFile(file) }
                    .onFailure { addon.logger.warn("[ConfigWorker] Failed to load ${file.name}: ${it.message}") }
            }

        // validate after both are loaded (cross-check slots etc.)
        validateAll(addon)
        addon.logger.info("[ConfigWorker] Loaded ${machinesMutable.size} machines, ${recipesMutable.values.sumOf { it.size }} recipes")
    }

    private fun loadMachinesFile(file: File) {
        val root = yaml.load<Map<String, Any?>>(file.readText()) ?: return
        for ((key, value) in root) {
            val map = value as? Map<*, *> ?: continue
            val id = key.trim()
            val model = map["model"]?.toString()
                ?: error("machine '$id' missing 'model' in ${file.name}")

            val invMap = (map["inventory"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val invType = when (invMap["type"]?.toString()?.lowercase()) {
                "hopper" -> InventoryKind.HOPPER
                "dispenser" -> InventoryKind.DISPENSER
                else -> InventoryKind.CHEST
            }
            val invSize = (invMap["size"] as? Number)?.toInt() ?: 27

            val slotsMap = (map["slots"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val input  = slotsMap["input"].asIntList()
            val output = slotsMap["output"].asIntList()
            val fuel   = slotsMap["fuel"].asIntList()

            val energyMap = (map["energy"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val energy = EnergySpec(
                enabled = (energyMap["enabled"] as? Boolean) ?: false,
                perTick = (energyMap["per_tick"] as? Number)?.toLong() ?: 0L,
                buffer  = (energyMap["buffer"] as? Number)?.toLong() ?: 1000L
            )

            val procMap = (map["processing"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val processing = ProcessingSpec(
                auto = (procMap["auto"] as? Boolean) ?: true,
                intervalTicks = (procMap["interval_ticks"] as? Number)?.toInt() ?: 200,
                toggleable = (procMap["toggleable"] as? Boolean) ?: true
            )

            val behMap = (map["behavior"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val behavior = BehaviorSpec(
                onOutputFull = when ((behMap["on_output_full"] ?: "block").toString().lowercase()) {
                    "block" -> OutputPolicy.BLOCK
                    else -> OutputPolicy.BLOCK
                },
                pauseIfNoEnergy = (behMap["pause_if_no_energy"] as? Boolean) ?: true
            )

            val guiMap = (map["gui"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val toggleMap = (guiMap["toggle"] as? Map<*, *>) ?: emptyMap<Any, Any>()
            val gui = GuiSpec(
                toggle = GuiToggleSpec(
                    enabled = (toggleMap["enabled"] as? Boolean) ?: true,
                    slot    = (toggleMap["slot"] as? Number)?.toInt() ?: 4,
                    onItem  = toggleMap["on_item"]?.toString() ?: "minecraft:lime_dye",
                    offItem = toggleMap["off_item"]?.toString() ?: "minecraft:red_dye",
                    onLabel = toggleMap["on_label"]?.toString() ?: "Auto: ON",
                    offLabel= toggleMap["off_label"]?.toString() ?: "Auto: OFF"
                )
            )

            val spec = MachineSpec(
                id = id,
                model = model,
                inventory = InventorySpec(invType, invSize),
                slots = SlotsSpec(input, output, fuel),
                energy = energy,
                processing = processing,
                behavior = behavior,
                gui = gui
            )

            if (machinesMutable.containsKey(id)) {
                throw IllegalArgumentException("duplicate machine id '$id' across files")
            }
            machinesMutable[id] = spec
        }
    }

    private fun loadRecipesFile(file: File) {
        val root = yaml.load<Map<String, Any?>>(file.readText()) ?: return
        for ((machineId, rawList) in root) {
            val list = (rawList as? List<*>) ?: emptyList<Any>()
            val parsed = list.mapNotNull { entry ->
                val m = entry as? Map<*, *> ?: return@mapNotNull null
                val name = m["name"]?.toString()
                val inputs  = (m["inputs"]  as? Map<*, *>)?.mapKeys { it.key.toString().toInt() }?.mapValues { it.value.toString().toEnc() } ?: emptyMap()
                val outputs = (m["outputs"] as? Map<*, *>)?.mapKeys { it.key.toString().toInt() }?.mapValues { it.value.toString().toEnc() } ?: emptyMap()
                val duration = (m["duration"] as? Number)?.toInt()
                val ept = (m["energy_per_tick"] as? Number)?.toLong()
                val durability = (m["durability"] as? Map<*, *>)?.mapKeys { it.key.toString().toInt() }?.mapValues { (it.value as? Number)?.toInt() ?: 0 } ?: emptyMap()
                RecipeSpec(name, inputs, outputs, duration, ept, durability)
            }
            // merge if multiple files define same machine key; append
            val existing = recipesMutable[machineId] ?: emptyList()
            recipesMutable[machineId] = existing + parsed
        }
    }

    private fun validateAll(addon: Addon) {
        for ((id, spec) in machinesMutable) {
            // inventory size
            val invSize = when (spec.inventory.type) {
                InventoryKind.HOPPER -> 5
                InventoryKind.DISPENSER -> 9
                InventoryKind.CHEST -> {
                    require(spec.inventory.size in chestSizes) {
                        "machine '$id': chest size must be one of $chestSizes"
                    }
                    spec.inventory.size
                }
            }

            // slots within bounds & unique
            fun checkSlots(name: String, slots: List<Int>) {
                slots.forEach {
                    require(it in 0 until invSize) { "machine '$id': slot $it in '$name' out of bounds 0..${invSize - 1}" }
                }
            }
            checkSlots("slots.input", spec.slots.input)
            checkSlots("slots.output", spec.slots.output)
            checkSlots("slots.fuel", spec.slots.fuel)

            val all = spec.slots.input + spec.slots.output + spec.slots.fuel
            require(all.size == all.toSet().size) { "machine '$id': input/output/fuel slots must be unique" }

            // toggle slot within bounds (only if enabled)
            if (spec.gui.toggle.enabled) {
                require(spec.gui.toggle.slot in 0 until invSize) { "machine '$id': gui.toggle.slot out of bounds for inventory" }
            }

            // recipes sanity (if present)
            val recipeList = recipesMutable[id] ?: emptyList()
            for (r in recipeList) {
                // inputs refer to declared input slots
                require(r.inputs.keys.all { it in spec.slots.input }) {
                    "machine '$id': recipe '${r.name ?: "<unnamed>"}' references input slots not in machine.slots.input"
                }
                // outputs refer to declared output slots
                require(r.outputs.keys.all { it in spec.slots.output }) {
                    "machine '$id': recipe '${r.name ?: "<unnamed>"}' references output slots not in machine.slots.output"
                }
                // durability refers to declared input slots
                require(r.durability.keys.all { it in spec.slots.input }) {
                    "machine '$id': recipe '${r.name ?: "<unnamed>"}' durability refers to non-input slots"
                }
            }
        }

        // warn if recipe exists for unknown machine
        for (mid in recipesMutable.keys) {
            if (mid !in machinesMutable.keys) {
                addon.logger.warn("[ConfigWorker] recipes file defines unknown machine id '$mid' (no matching machine in /machines)")
            }
        }
    }

    // ======== helpers =========================================================

    private fun Any?.asIntList(): List<Int> =
        (this as? List<*>)?.mapNotNull { (it as? Number)?.toInt() } ?: emptyList()

    private fun String.toEnc(): EncodedItem {
        val star = lastIndexOf('*')
        return if (star > 0) {
            val id = substring(0, star)
            val amt = substring(star + 1).toIntOrNull() ?: 1
            EncodedItem(id, amt.coerceAtLeast(1))
        } else EncodedItem(this, 1)
    }

    // Public getters
    fun getMachine(id: String): MachineSpec? = machinesMutable[id]
    fun getRecipesFor(id: String): List<RecipeSpec> = recipesMutable[id] ?: emptyList()
}