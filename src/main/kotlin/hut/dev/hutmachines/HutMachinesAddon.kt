package hut.dev.hutmachines

import hut.dev.hutmachines.workers.ConfigWorker
import hut.dev.hutmachines.workers.MachineSpecRegistry
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitStage

object HutMachinesAddon : Addon()

@Init(stage = InitStage.PRE_PACK)
object HutMachinesBootstrap {
    @InitFun
    fun loadAndPrepare() {
        ConfigWorker.loadAll(HutMachinesAddon)
        HutMachinesAddon.logger.info("Config loaded: ${ConfigWorker.machines.size} machines")
        MachineSpecRegistry.registerAll(HutMachinesAddon)
    }
}