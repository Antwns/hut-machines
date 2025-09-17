// src/main/kotlin/hut/dev/hutmachines/workers/MachineInstanceRegistryWorker.kt
package hut.dev.hutmachines.workers

import hut.dev.hutmachines.core.BlockKey
import hut.dev.hutmachines.core.MachineContext
import hut.dev.hutmachines.model.MachineSpec
import hut.dev.hutmachines.model.RecipeSpec
import org.bukkit.World
import xyz.xenondevs.nova.addon.Addon
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object MachineInstanceRegistryWorker {

    private val _instances = ConcurrentHashMap<BlockKey, MachineContext>()
    val instances: Map<BlockKey, MachineContext> get() = _instances

    fun createInstance(
        addon: Addon,
        machineId: String,
        spec: MachineSpec,
        recipes: List<RecipeSpec>,
        world: World,
        x: Int, y: Int, z: Int,
        owner: UUID?
    ): MachineContext {
        val ctx = MachineContext(addon, machineId, spec, recipes, world, x, y, z, owner)
        _instances[ctx.blockKey()] = ctx
        return ctx
    }

    fun get(worldName: String, x: Int, y: Int, z: Int): MachineContext? =
        _instances[BlockKey(worldName, x, y, z)]

    fun remove(worldName: String, x: Int, y: Int, z: Int): MachineContext? =
        _instances.remove(BlockKey(worldName, x, y, z))
}