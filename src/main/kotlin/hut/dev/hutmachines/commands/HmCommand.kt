package hut.dev.hutmachines.commands

import hut.dev.hutmachines.workers.MachineInstanceRegistryWorker
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object HmCommand : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.hasPermission("hutmachines.admin")) {
            sender.sendMessage("§cNo permission.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /$label <delete>")
            return true
        }

        when (args[0].lowercase()) {
            "delete", "remove" -> {
                if (sender !is Player) {
                    sender.sendMessage("§cOnly players can use this.")
                    return true
                }

                // no raytracing — exact target up to 6 blocks
                val target = sender.getTargetBlockExact(6)
                if (target == null) {
                    sender.sendMessage("§eLook at a block within 6 blocks.")
                    return true
                }

                val loc = target.location
                val worldName = loc.world?.name ?: run {
                    sender.sendMessage("§cWorld missing.")
                    return true
                }

                val removed = MachineInstanceRegistryWorker.remove(worldName, loc.blockX, loc.blockY, loc.blockZ)
                if (removed == null) {
                    sender.sendMessage("§7No HutMachine at §f${worldName} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
                } else {
                    sender.sendMessage("§aHutMachine removed at §f${worldName} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
                }
                return true
            }

            else -> {
                sender.sendMessage("§eUnknown subcommand. Try /$label delete")
                return true
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) {
            val options = listOf("delete")
            return StringUtil.copyPartialMatches(args[0], options, mutableListOf())
        }
        return mutableListOf()
    }
}