package hut.dev.hutmachines.commands

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineInstanceRegistryWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

fun buildHmRoot(plugin: JavaPlugin): LiteralCommandNode<CommandSourceStack> {
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
            val target = sender.getTargetBlockExact(6) ?: run {
                sender.sendMessage("§eLook at a block within 6 blocks to get the relevant information.")
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

    val markNode = LiteralArgumentBuilder.literal<CommandSourceStack>("mark")
        .then(
            RequiredArgumentBuilder.argument<CommandSourceStack, String>("id", StringArgumentType.word())
                .suggests { _, b ->
                    MachineSpecRegistry.machines.keys.forEach { b.suggest(it) }
                    b.buildFuture()
                }
                .executes { ctx ->
                    val sender = ctx.source.sender
                    if (sender !is Player) {
                        sender.sendMessage("§cOnly players can use this.")
                        return@executes 0
                    }

                    val id = StringArgumentType.getString(ctx, "id")
                    val reg = MachineSpecRegistry.get(id) ?: run {
                        sender.sendMessage("§cUnknown machine id: §f$id")
                        return@executes 0
                    }

                    val target = sender.getTargetBlockExact(6) ?: run {
                        sender.sendMessage("§eLook at a block within 6 blocks.")
                        return@executes 0
                    }

                    val loc = target.location
                    val world = loc.world ?: run {
                        sender.sendMessage("§cWorld missing.")
                        return@executes 0
                    }

                    MachineInstanceRegistryWorker.createInstance(
                        plugin = plugin,                   // <-- pass it here
                        machineId = id,
                        spec = reg.spec,
                        recipes = ConfigWorker.getRecipesFor(id),
                        world = world,
                        x = loc.blockX, y = loc.blockY, z = loc.blockZ,
                        owner = sender.uniqueId
                    )

                    sender.sendMessage("§aHutMachine §f$id §acreated at §f${world.name} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
                    1
                }
        )
        .build()

    // --- /hm info ---
    val infoNode = LiteralArgumentBuilder.literal<CommandSourceStack>("info")
        .executes { ctx ->
            val sender = ctx.source.sender
            if (sender !is Player) {
                sender.sendMessage("§cOnly players can use this.")
                return@executes 0
            }

            val target = sender.getTargetBlockExact(6) ?: run {
                sender.sendMessage("§eLook at a block within 6 blocks.")
                return@executes 0
            }

            val loc = target.location
            val worldName = loc.world?.name ?: return@executes 0
            val inst = MachineInstanceRegistryWorker.get(worldName, loc.blockX, loc.blockY, loc.blockZ)
            if (inst == null) {
                sender.sendMessage("§7No HutMachine at §f${worldName} ${loc.blockX},${loc.blockY},${loc.blockZ}.")
                return@executes 0
            }

            val spec = inst.spec
            val recipes = inst.recipes
            val ownerStr = inst.owner?.toString()?.take(8) ?: "none"

            sender.sendMessage("§aHutMachine info:")
            sender.sendMessage(" §7id: §f${inst.machineId}")
            sender.sendMessage(" §7model: §f${spec.model}")
            sender.sendMessage(" §7inv: §f${spec.inventory.type} ${spec.inventory.size}")
            sender.sendMessage(" §7slots: §fin=${spec.slots.input} out=${spec.slots.output} fuel=${spec.slots.fuel}")
            sender.sendMessage(" §7processing: §fauto=${spec.processing.auto} interval=${spec.processing.intervalTicks}t")
            sender.sendMessage(" §7recipes: §f${recipes.size}")
            sender.sendMessage(" §7owner: §f$ownerStr")
            1
        }
        .build()

    root.addChild(deleteNode)
    root.addChild(markNode)
    root.addChild(infoNode)
    return root
}