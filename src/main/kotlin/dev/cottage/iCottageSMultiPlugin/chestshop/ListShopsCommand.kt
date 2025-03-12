package dev.cottage.iCottageSMultiPlugin.chestshop

import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.sqrt

class ListShopsCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Ensure the sender is a player
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players!")
            return false
        }

        val player = sender

        // No shops to list
        if (iCottageSMultiPlugin.chestShops.isEmpty()) {
            player.sendMessage("§cThere are no shops to list.")
            return true
        }

        // Get player's current world
        val playerWorld = player.world.name

        // Filter shops that are in the same dimension as the player
        val shopsInSameDimension = iCottageSMultiPlugin.chestShops.filter { shop ->
            val shopLocation = shop.chestLocation
            val worldStart = shopLocation.indexOf("world=") + 6
            val worldEnd = shopLocation.indexOf(",", worldStart)
            val worldName = shopLocation.substring(worldStart, worldEnd).replace("CraftWorld{name=", "").replace("}", "")

            worldName == playerWorld
        }

        if (shopsInSameDimension.isEmpty()) {
            player.sendMessage("§cThere are no shops in this dimension.")
            return true
        }

        // Default to proximity sorting if no args provided
        val sortType = if (args.isNotEmpty()) args[0].lowercase(Locale.getDefault()) else "proximity"

        // Check if we're filtering by a specific item
        val filterItemName = if (sortType == "item" && args.size > 1) args[1].uppercase(Locale.getDefault()) else null

        // Parse shop coordinates for sorting by proximity
        val shopsWithCoords = shopsInSameDimension.map { shop ->
            val shopLocation = shop.chestLocation

            // Extract x, y, z coordinates
            val xStart = shopLocation.indexOf("x=") + 2
            val xEnd = shopLocation.indexOf(",", xStart)
            val x = shopLocation.substring(xStart, xEnd).toDouble().toInt()

            val yStart = shopLocation.indexOf("y=") + 2
            val yEnd = shopLocation.indexOf(",", yStart)
            val y = shopLocation.substring(yStart, yEnd).toDouble().toInt()

            val zStart = shopLocation.indexOf("z=") + 2
            val zEnd = shopLocation.indexOf(",", zStart)
            val z = shopLocation.substring(zStart, zEnd).toDouble().toInt()

            Triple(shop, x to y to z, distanceTo(player.location.x, player.location.y, player.location.z, x.toDouble(), y.toDouble(), z.toDouble()))
        }

        // Filter by specific item if provided
        val filteredShops = if (filterItemName != null) {
            // Try to convert the string to Material
            try {
                val material = Material.valueOf(filterItemName)
                shopsWithCoords.filter { it.first.sellItem.type == material }
            } catch (e: IllegalArgumentException) {
                // If item name is invalid, show a message and return
                player.sendMessage("§cInvalid item name: $filterItemName")
                return true
            }
        } else {
            shopsWithCoords
        }

        // Check if there are no shops after filtering
        if (filteredShops.isEmpty()) {
            if (filterItemName != null) {
                player.sendMessage("§cThere are no shops selling ${filterItemName.replace('_', ' ').lowercase()} in this dimension.")
            } else {
                player.sendMessage("§cThere are no shops in this dimension.")
            }
            return true
        }

        // Sort shops based on sort type
        val sortedShops = if (sortType == "proximity") {
            // Sort by proximity
            filteredShops.sortedBy { it.third }
        } else {
            // Sort by item being sold
            filteredShops.sortedBy { it.first.sellItem.type.name }
        }

        // Display the results
        if (filterItemName != null) {
            player.sendMessage("§6===== Shops selling ${filterItemName.replace('_', ' ').lowercase()} (Sorted by $sortType) =====")
        } else {
            player.sendMessage("§6===== Shops (Sorted by $sortType) =====")
        }

        sortedShops.forEach { (shop, coords, distance) ->
            val (x, y) = coords.first
            val z = coords.second
            val ownerName = try {
                Bukkit.getOfflinePlayer(shop.owner).name ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }

            val formattedDistance = String.format("%.1f", distance)

            player.sendMessage("§a${shop.sellAmount} §f${shop.sellItem.type}§a for §f${shop.buyAmount} ${shop.buyItem.type} §7- §eOwner: §f$ownerName §7- §eCoords: §f$x, $y, $z §7(§f${formattedDistance}m§7)")
        }

        return true
    }

    // Helper method to calculate distance between two points
    private fun distanceTo(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dz = z2 - z1
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
