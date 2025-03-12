package dev.cottage.iCottageSMultiPlugin.chestshop

import ChestShop
import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.Bukkit
import org.bukkit.inventory.InventoryHolder
import org.bukkit.plugin.Plugin
import org.bukkit.event.block.Action

class ChestShopListener : Listener {

    // Create a custom inventory holder to store shop information
    class ShopInventoryHolder(val chestShop: ChestShop) : InventoryHolder {
        override fun getInventory(): Inventory {
            // This is just a placeholder implementation
            return Bukkit.createInventory(this, 9)
        }
    }

    // Event handler for when a player interacts with a block
    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only process right clicks on blocks
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val clickedBlock = event.clickedBlock ?: return

        // Check if the clicked block is a chest and if it's associated with a shop
        val blockLocation = clickedBlock.location.toString()
        val chestShop = iCottageSMultiPlugin.chestShops.find {
            it.chestLocation == blockLocation
        }

        if (chestShop != null) {
            // If player is the owner, show the chest storage with items and price
            if (player.uniqueId == chestShop.owner) {
                event.isCancelled = false
            } else {
                // If player is not the owner, show a GUI to buy
                event.isCancelled = true
                showPurchaseGUI(player, chestShop)
            }
        }
    }

    // Show the purchase confirmation GUI to non-owners
    private fun showPurchaseGUI(player: Player, chestShop: ChestShop) {
        // Create inventory with custom holder that stores shop data
        val holder = ShopInventoryHolder(chestShop)
        val buyGUI: Inventory = Bukkit.createInventory(holder, 9, "Buy Confirmation")

        // Get the price for the sell item
        val totalCost = "${chestShop.buyAmount} ${chestShop.buyItem.type}"

        // Add items for the GUI: "Buy" button and the product details
        val buyButton = ItemStack(Material.GREEN_WOOL, 1)
        val cancelButton = ItemStack(Material.RED_WOOL, 1)
        val buyMeta = buyButton.itemMeta
        buyMeta?.setDisplayName("Buy ${chestShop.sellAmount} ${chestShop.sellItem.type} for $totalCost")
        buyButton.itemMeta = buyMeta

        val cancelMeta = cancelButton.itemMeta
        cancelMeta?.setDisplayName("Cancel")
        cancelButton.itemMeta = cancelMeta

        buyGUI.setItem(3, buyButton) // Place the Buy button in the center slot
        buyGUI.setItem(5, cancelButton)

        // Open the purchase GUI for the player
        player.openInventory(buyGUI)
    }

    // Handle GUI interactions (when a player clicks on the Buy button)
    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val clickedItem = event.currentItem ?: return

        if (event.view.title == "Buy Confirmation") {
            // Cancel the event to prevent items from being moved in the inventory
            event.isCancelled = true

            // Get the shop from our inventory holder
            val holder = event.inventory.holder as? ShopInventoryHolder
            if (holder == null) {
                player.sendMessage("Debug: Holder is null")
                player.closeInventory()
                return
            }

            val chestShop = holder.chestShop

            // Check if the player clicked the Buy button
            if (clickedItem.type == Material.GREEN_WOOL) {
                // Get the player's currency
                val playerCurrency = player.inventory.contents.filter { it?.type == chestShop.buyItem.type }
                val sellAmount = chestShop.sellAmount
                val buyAmount = chestShop.buyAmount

                // Calculate the total cost of the purchase
                val totalCost = buyAmount
                val playerCurrencyAmount = playerCurrency.sumOf { it?.amount ?: 0 }

                // Check if the player has enough currency
                if (playerCurrencyAmount >= totalCost) {
                    // Check if the player has space for the item they are buying
                    if (player.inventory.firstEmpty() != -1) {
                        // Parse the chest location
                        val locationString = chestShop.chestLocation

                        // Extract the world name
                        val worldStart = locationString.indexOf("world=") + 6
                        val worldEnd = locationString.indexOf(",", worldStart)
                        val worldName = locationString.substring(worldStart, worldEnd).replace("CraftWorld{name=", "").replace("}", "")

                        // Extract x, y, z coordinates
                        val xStart = locationString.indexOf("x=") + 2
                        val xEnd = locationString.indexOf(",", xStart)
                        val x = locationString.substring(xStart, xEnd).toDouble().toInt()

                        val yStart = locationString.indexOf("y=") + 2
                        val yEnd = locationString.indexOf(",", yStart)
                        val y = locationString.substring(yStart, yEnd).toDouble().toInt()

                        val zStart = locationString.indexOf("z=") + 2
                        val zEnd = locationString.indexOf(",", zStart)
                        val z = locationString.substring(zStart, zEnd).toDouble().toInt()

                        // Get the chest block using the parsed location
                        val world = Bukkit.getWorld(worldName)
                        if (world == null) {
                            player.sendMessage("Error: World not found! Debug: $worldName")
                            player.closeInventory()
                            return
                        }

                        val chestBlock = world.getBlockAt(x, y, z)
                        if (chestBlock.type != Material.CHEST) {
                            player.sendMessage("Error: Chest not found! Debug: $x,$y,$z")
                            player.closeInventory()
                            return
                        }

                        val chestInventory = (chestBlock.state as? org.bukkit.block.Chest)?.inventory
                        if (chestInventory == null) {
                            player.sendMessage("Error: Chest inventory not found!")
                            player.closeInventory()
                            return
                        }

                        // Check if chest has enough of the sell item
                        val hasEnoughItems = chestInventory.contains(chestShop.sellItem.type, sellAmount)

                        if (!hasEnoughItems) {
                            player.sendMessage("The shop does not have enough stock!")

                            // Notify owner that shop is empty
                            val owner = Bukkit.getPlayer(chestShop.owner)
                            owner?.sendMessage("§6[Shop Alert] §cYour shop at §f$x, $y, $z §cis out of stock §f(${chestShop.sellItem.type})§c!")

                            player.closeInventory()
                            return
                        }

                        // Create an ItemStack with the exact sell item from the shop configuration
                        val itemToSell = ItemStack(chestShop.sellItem.type, sellAmount)

                        // Check if the chest has room for the currency
                        val currencyToRemove = ItemStack(chestShop.buyItem.type, totalCost)
                        val hasRoomForCurrency = hasRoomForItem(chestInventory, currencyToRemove)

                        if (!hasRoomForCurrency) {
                            player.sendMessage("§cThe shop is full and cannot accept payment. Please notify the shop owner.")

                            // Notify owner that shop is full
                            val owner = Bukkit.getPlayer(chestShop.owner)
                            owner?.sendMessage("§6[Shop Alert] §cYour shop at §f$x, $y, $z §cis full and cannot accept payments!")

                            player.closeInventory()
                            return
                        }

                        // Remove the currency from the player's inventory
                        val remainingCurrency = player.inventory.removeItem(currencyToRemove)

                        // If the currency was removed, proceed with the transaction
                        if (remainingCurrency.isEmpty()) {
                            // Remove the sold items from the chest
                            chestInventory.removeItem(itemToSell)

                            // Add the purchased item to the player's inventory
                            player.inventory.addItem(itemToSell)

                            // Add the currency to the chest
                            chestInventory.addItem(currencyToRemove)

                            // Notify the player of the successful purchase
                            player.sendMessage("You bought $sellAmount ${chestShop.sellItem.type} for $totalCost ${chestShop.buyItem.type}!")

                            // Notify the shop owner of the purchase
                            val owner = Bukkit.getPlayer(chestShop.owner)
                            owner?.sendMessage("§6[Shop Alert] §a${player.name} bought §f$sellAmount ${chestShop.sellItem.type} §afor §f$totalCost ${chestShop.buyItem.type} §afrom your shop at §f$x, $y, $z§a!")

                            player.closeInventory()
                        } else {
                            player.sendMessage("You don't have enough currency to complete the purchase!")
                            player.closeInventory()
                        }
                    } else {
                        player.sendMessage("You don't have enough space in your inventory!")
                        player.closeInventory()
                    }
                } else {
                    // Notify the player if they don't have enough currency
                    player.sendMessage("You don't have enough currency for this purchase!")
                    player.closeInventory()
                }
            }
            else if (clickedItem.type == Material.RED_WOOL) {
                player.sendMessage("Purchase canceled!")
                player.closeInventory()
            }
        }
    }

    // Helper method to check if an inventory has room for an item
    private fun hasRoomForItem(inventory: Inventory, item: ItemStack): Boolean {
        // Create a copy of the inventory to simulate adding the item
        val tempInventory = Bukkit.createInventory(null, inventory.size)
        for (i in 0 until inventory.size) {
            val slotItem = inventory.getItem(i)
            if (slotItem != null) {
                tempInventory.setItem(i, slotItem.clone())
            }
        }

        // Try to add the item to the temp inventory
        val remainingItems = tempInventory.addItem(item.clone())

        // If there are no remaining items, the inventory has room
        return remainingItems.isEmpty()
    }
}
