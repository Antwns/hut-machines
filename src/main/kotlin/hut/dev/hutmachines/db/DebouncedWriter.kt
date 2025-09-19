package hut.dev.hutmachines.db

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DebouncedWriter(private val plugin: Plugin, private val delayTicks: Long = 10L) {
    private val scheduled = ConcurrentHashMap<UUID, Int>()

    fun requestFlush(machineId: UUID, action: () -> Unit) {
        scheduled.remove(machineId)?.let { Bukkit.getScheduler().cancelTask(it) }
        val id = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, Runnable {
            scheduled.remove(machineId)
            action()
        }, delayTicks).taskId
        scheduled[machineId] = id
    }

    fun flushNow(machineId: UUID, action: () -> Unit) {
        scheduled.remove(machineId)?.let { Bukkit.getScheduler().cancelTask(it) }
        action()
    }

    fun flushAll(actionById: (UUID) -> Unit) {
        val ids = scheduled.keys.toList()
        ids.forEach { scheduled.remove(it)?.let(Bukkit.getScheduler()::cancelTask) }
        ids.forEach(actionById)
    }
}
