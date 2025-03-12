import org.bukkit.inventory.ItemStack
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.block.Chest
import org.bukkit.inventory.Inventory

data class ChestShop(
    val chestLocation: String,
    val sellItem: ItemStack,
    val buyItem: ItemStack,
    val sellAmount: Int,
    val buyAmount: Int,
    val owner: UUID
) {
    fun getChestInventory(): Inventory? {
        val loc = Bukkit.getWorld("world")?.getBlockAt(
            chestLocation.split(",")[0].toInt(),
            chestLocation.split(",")[1].toInt(),
            chestLocation.split(",")[2].toInt()
        ) ?: return null

        return (loc.state as? Chest)?.inventory
    }
}
