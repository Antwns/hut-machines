package hut.dev.hutmachines.commands

import hut.dev.hutmachines.HutMachinesAddon
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineInstanceRegistryWorker
import org.bukkit.Bukkit
import org.bukkit.FluidCollisionMode
import org.bukkit.block.BlockFace
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class HmCommand(
    private val addon: HutMachinesAddon // we only need logger
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Players only.")
            return true
        }
        if (!sender.hasPermission("hutmachines.command")) {
            sender.sendMessage("No permission.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Usage: /hm <make|here|delete> [machineId]")
            return true
        }

        when (args[0].lowercase()) {
            "make" -> {
                val machineId = args.getOrNull(1)
                if (machineId == null) {
                    sender.sendMessage("Usage: /hm make <machineId>")
                    return true
                }
                val spec = ConfigWorker.getMachine(machineId)
                if (spec == null) {
                    sender.sendMessage("Unknown machineId '$machineId'.")
                    return true
                }
                val recipes = ConfigWorker.getRecipesFor(machineId)

                val maxDist = 6
                val target = sender.getTargetBlockExact(maxDist, FluidCollisionMode.NEVER)
                if (target == null) {
                    sender.sendMessage("Aim at a solid block within $maxDist blocks.")
                    return true
                }
                val face = sender.getTargetBlockFace(maxDist, FluidCollisionMode.NEVER) ?: BlockFace.UP
                val placeAt = target.getRelative(face)

                if (!placeAt.type.isAir) {
                    sender.sendMessage("That spot is not free (aim at a block face with air on that side).")
                    return true
                }

                val ctx = MachineInstanceRegistryWorker.createInstance(
                    addon = addon,
                    machineId = machineId,
                    spec = spec,
                    recipes = recipes,
                    world = placeAt.world,
                    x = placeAt.x,
                    y = placeAt.y,
                    z = placeAt.z,
                    owner = sender.uniqueId
                )

                sender.sendMessage("Created instance '$machineId' at ${placeAt.world.name} ${placeAt.x},${placeAt.y},${placeAt.z}.")
                addon.logger.info("[/hm make] ${sender.name} -> $machineId @ ${placeAt.world.name} ${placeAt.x},${placeAt.y},${placeAt.z}")
            }

            "here" -> {
                val maxDist = 6
                val target = sender.getTargetBlockExact(maxDist, FluidCollisionMode.NEVER)
                if (target == null) {
                    sender.sendMessage("Aim at a block within $maxDist blocks.")
                    return true
                }
                val face = sender.getTargetBlockFace(maxDist, FluidCollisionMode.NEVER) ?: BlockFace.UP
                val pos = target.getRelative(face)

                val ctx = MachineInstanceRegistryWorker.get(pos.world.name, pos.x, pos.y, pos.z)
                if (ctx == null) {
                    sender.sendMessage("No machine instance at ${pos.world.name} ${pos.x},${pos.y},${pos.z}.")
                } else {
                    sender.sendMessage("Found '${ctx.machineId}' [auto=${ctx.state.autoEnabled}] at ${pos.world.name} ${pos.x},${pos.y},${pos.z}.")
                }
            }

            "delete" -> {
                val maxDist = 6
                val target = sender.getTargetBlockExact(maxDist, FluidCollisionMode.NEVER)
                if (target == null) {
                    sender.sendMessage("Aim at a block within $maxDist blocks.")
                    return true
                }
                val face = sender.getTargetBlockFace(maxDist, FluidCollisionMode.NEVER) ?: BlockFace.UP
                val pos = target.getRelative(face)

                val removed = MachineInstanceRegistryWorker.remove(pos.world.name, pos.x, pos.y, pos.z)
                if (removed == null) {
                    sender.sendMessage("Nothing to delete at ${pos.world.name} ${pos.x},${pos.y},${pos.z}.")
                } else {
                    sender.sendMessage("Deleted instance '${removed.machineId}' at ${pos.world.name} ${pos.x},${pos.y},${pos.z}.")
                    addon.logger.info("[/hm delete] ${sender.name} removed '${removed.machineId}' at ${pos.world.name} ${pos.x},${pos.y},${pos.z}")
                }
            }

            else -> sender.sendMessage("Usage: /hm <make|here|delete> [machineId]")
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, cmd: Command, alias: String, args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            return listOf("make", "here", "delete")
                .filter { it.startsWith(args[0], ignoreCase = true) }
                .toMutableList()
        }
        if (args.size == 2 && args[0].equals("make", ignoreCase = true)) {
            return ConfigWorker.machines.keys
                .filter { it.startsWith(args[1], ignoreCase = true) }
                .toMutableList()
        }
        return mutableListOf()
    }

    companion object {
        fun register(addon: HutMachinesAddon) {
            val cmd = Bukkit.getPluginCommand("hm")
            if (cmd == null) {
                addon.logger.warn("Command 'hm' not defined in paper-plugin.yml")
                return
            }
            val exec = HmCommand(addon)
            cmd.setExecutor(exec)
            cmd.tabCompleter = exec
        }
    }
}