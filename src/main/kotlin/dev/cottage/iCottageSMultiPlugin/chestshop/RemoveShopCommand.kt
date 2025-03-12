package dev.cottage.iCottageSMultiPlugin.chestshop

import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class RemoveShopCommand : CommandExecutor {
    // Method to handle the /removeshop command
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Ensure the sender is a player
        if (sender is Player) {
            // Check if the player is looking at a chest within 5 blocks
            val block = sender.getTargetBlock(null, 5) // Get the block the player is looking at (within a range of 5 blocks)
            if (block == null || block.type != Material.CHEST) {
                sender.sendMessage("You must be looking at a chest to remove a shop!")
                return false // Return false if the player is not looking at a chest
            }

            // Get the location of the chest as a string
            val chestLocation = block.location.toString()


            // Find the chest shop based on the chest's location
            val chestShop = iCottageSMultiPlugin.chestShops.find { it.chestLocation == chestLocation }
            if (chestShop != null) {
                // If the chest shop exists, check if the player is the owner of the shop
                if (chestShop.owner != sender.uniqueId) {
                    sender.sendMessage("You are not the owner of this shop!")
                    return false // Return false if the player is not the owner
                }

                // If the player is the owner, remove the chest shop from the list
                iCottageSMultiPlugin.chestShops.remove(chestShop)
                sender.sendMessage("Shop removed at the chest you're looking at!")
            } else {
                // If no chest shop was found, inform the player
                sender.sendMessage("No shop found at the chest you're looking at!")
            }
            return true // Command successfully processed
        }
        return false // Return false if the sender is not a player
    }
}
