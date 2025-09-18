package hut.dev.hutmachines.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import hut.dev.hutmachines.workers.MachineInstanceRegistryWorker

fun buildHmRoot(): LiteralCommandNode<CommandSourceStack> {
    val root = LiteralArgumentBuilder.literal<CommandSourceStack>("hm")
        .requires { src -> src.sender.hasPermission("hutmachines.admin") }
        .build()

    val deleteNode = LiteralArgumentBuilder.literal<CommandSourceStack>("delete")
        .executes { ctx ->
            val sender = ctx.source.sender
            if (sender !is Player) {
                sender.sendMessage("§cOnly players can use this.")
                return@executes 0
            }

            val target = sender.getTargetBlockExact(6)
            if (target == null) {
                sender.sendMessage("§eLook at a block within 6 blocks.")
                return@executes 0
            }

            val loc = target.location
            val worldName = loc.world?.name ?: return@executes 0

            val removed = MachineInstanceRegistryWorker.remove(worldName, loc.blockX, loc.blockY, loc.blockZ)
            if (removed == null) {
                sender.sendMessage("§7No HutMachine at §f${worldName} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
                return@executes 0
            }

            sender.sendMessage("§aHutMachine removed at §f${worldName} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
            1
        }
        .build()

    root.addChild(deleteNode)
    return root
}