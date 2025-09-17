package hut.dev.hutmachines.util

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

object Durability {

    /**
     * Try to damage the stack by [amount]. If it breaks, consume 1 item from the stack.
     * Returns true if we applied damage or consumed; false if the item isn’t damageable.
     */
    fun damageOrConsume(stack: ItemStack, amount: Int): Boolean {
        if (amount <= 0) return true
        val meta = stack.itemMeta as? Damageable ?: return false

        val max = stack.type.maxDurability.toInt()
        if (max <= 0) return false

        val newDamage = meta.damage + amount
        return if (newDamage >= max) {
            // break one
            stack.amount -= 1
            if (stack.amount <= 0) {
                stack.type = Material.AIR
            } else {
                // reset damage on next item
                val m2 = stack.itemMeta as? Damageable
                if (m2 != null) {
                    m2.damage = 0
                    stack.itemMeta = m2
                }
            }
            true
        } else {
            meta.damage = newDamage
            stack.itemMeta = meta
            true
        }
    }
}