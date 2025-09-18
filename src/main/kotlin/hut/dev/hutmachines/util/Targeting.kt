package hut.dev.hutmachines.util

import org.bukkit.FluidCollisionMode
import org.bukkit.block.Block
import org.bukkit.entity.Player

object Targeting {
    /**
     * Returns the exact block the player is looking at, up to [range] blocks.
     * Air-only misses return null. Fluids are ignored.
     */
    fun getTargetBlock(player: Player, range: Int = 6): Block? =
        player.getTargetBlockExact(range, FluidCollisionMode.NEVER)
}