package hut.dev.hutmachines.ia

import dev.lone.itemsadder.api.CustomStack
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

object IaCompat {

    /** Create an ItemStack from a namespaced id. Supports vanilla or ItemsAdder. */
    fun fromId(id: String, amount: Int = 1): ItemStack {
        if (id.isBlank()) return ItemStack(Material.AIR)
        val amt = amount.coerceAtLeast(1)

        return if (id.startsWith("minecraft:", ignoreCase = true)) {
            val key = id.substringAfter(':')
            val mat = Material.matchMaterial(key) ?: Material.BARRIER
            ItemStack(mat, amt)
        } else {
            // itemsadder:<something>
            try {
                val cs = CustomStack.getInstance(id)
                if (cs != null) {
                    cs.itemStack.clone().apply { this.amount = amt }
                } else ItemStack(Material.AIR)
            } catch (_: Throwable) {
                ItemStack(Material.AIR)
            }
        }
    }

    /** Check if a stack matches a namespaced id (ignores amount unless you pass minAmount). */
    fun matches(stack: ItemStack?, id: String, minAmount: Int = 1): Boolean {
        if (stack == null || stack.type.isAir) return false

        return if (id.startsWith("minecraft:", ignoreCase = true)) {
            val key = id.substringAfter(':')
            stack.type.name.equals(key, ignoreCase = true) && stack.amount >= minAmount
        } else {
            try {
                val cs = CustomStack.byItemStack(stack)
                cs?.namespacedID.equals(id, ignoreCase = true) && stack.amount >= minAmount
            } catch (_: Throwable) {
                false
            }
        }
    }
}