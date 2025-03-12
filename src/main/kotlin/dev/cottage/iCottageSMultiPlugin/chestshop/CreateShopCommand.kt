package dev.cottage.iCottageSMultiPlugin.chestshop

import ChestShop
import java.util.logging.Logger
import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

class CreateShopCommand : CommandExecutor {
    // This method handles the /createshop command logic
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        // Ensure the sender is a player
        if (sender is Player) {
            // Check if the correct number of arguments are provided (4 arguments)
            if (args.size != 4) {
                sender.sendMessage("Usage: /createshop <sellItem> <buyItem> <sellAmount> <buyAmount>")
                return false
            }

            // Parse and validate the items and amounts from the arguments
            val sellItem = Material.matchMaterial(args[0]) // Get the material for the sell item
            val buyItem = Material.matchMaterial(args[1])  // Get the material for the buy item
            val sellAmount = args[2].toIntOrNull()         // Convert sell amount to integer
            val buyAmount = args[3].toIntOrNull()          // Convert buy amount to integer

            // Ensure valid items and amounts are provided
            if (sellItem == null || buyItem == null || sellAmount == null || buyAmount == null) {
                sender.sendMessage("Invalid item or amount!")
                return false
            }

            // Check if the player is looking at a chest within 5 blocks
            val block = sender.getTargetBlock(null, 5) // Get the block the player is looking at (within a range of 5 blocks)
            if (block == null || block.type != Material.CHEST) {
                sender.sendMessage("You must be looking at a chest to remove a shop!")
                return false // Return false if the player is not looking at a chest
            }

            // Store the shop at the chest location with the player's name as the owner
            val chestLocation = block.location.toString()

            // Create the ChestShop instance
            val chestShop = ChestShop(
                chestLocation,
                ItemStack(sellItem, sellAmount),  // Create an ItemStack for the sell item with the amount
                ItemStack(buyItem, buyAmount),    // Create an ItemStack for the buy item with the amount
                sellAmount,                       // Amount of sell item
                buyAmount,                        // Amount of buy item
                sender.uniqueId                   // The player creating the shop is the owner
            )

            // Add the created shop to the list of chest shops
            iCottageSMultiPlugin.chestShops.add(chestShop)

            // Notify the player that the shop was created successfully
            sender.sendMessage("Shop created at the chest you're looking at! Selling $sellAmount $sellItem for $buyAmount $buyItem.")
            return true
        }
        // Return false if the sender is not a player (e.g., command issued by console)
        else {
            sender.sendMessage("You must be a player to use this command!")
            return false
        }
    }
}
