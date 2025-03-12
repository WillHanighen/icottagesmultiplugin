package dev.cottage.iCottageSMultiPlugin.chestshop

import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.inventory.InventoryHolder
import org.bukkit.block.Chest
import org.bukkit.entity.Player
import org.bukkit.block.Block
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.block.Action
import org.bukkit.inventory.ItemStack
import org.bukkit.block.DoubleChest
import org.bukkit.event.inventory.InventoryType

class ChestProtectionListener : Listener {

    private val config = iCottageSMultiPlugin.instance.config // Load the configuration
    private val ADJACENT_FACES = arrayOf(
        BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
        BlockFace.UP, BlockFace.DOWN
    )

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block

        // Check if breaking a chest directly
        if (block.type == Material.CHEST) {
            if (isProtectedChest(block, event.player)) {
                event.isCancelled = true
                event.player.sendMessage(config.getString("chestProtection.messages.cannotBreakProtectedChest"))
                return
            }

            // If player is the owner, allow breaking and remove the shop
            val chestLocation = block.location.toString()
            val chestShop = iCottageSMultiPlugin.chestShops.find { it.chestLocation == chestLocation }

            if (chestShop != null && chestShop.owner == event.player.uniqueId) {
                iCottageSMultiPlugin.chestShops.remove(chestShop)
                iCottageSMultiPlugin.instance.saveChestShops()
                event.player.sendMessage(config.getString("chestProtection.messages.shopRemoved"))
            }
        }

        // Check if breaking a block adjacent to a protected chest
        else {
            for (face in ADJACENT_FACES) {
                val adjacent = block.getRelative(face)
                if (adjacent.type == Material.CHEST && isProtectedChest(adjacent, event.player)) {
                    event.isCancelled = true
                    event.player.sendMessage(config.getString("chestProtection.messages.cannotBreakAdjacent"))
                    return
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val block = event.block
        val player = event.player

        // Check if player is placing a chest
        if (block.type == Material.CHEST) {
            // Check if placing next to an existing chest to form a double chest
            for (face in arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
                val adjacent = block.getRelative(face)
                if (adjacent.type == Material.CHEST && isProtectedChest(adjacent, player)) {
                    event.isCancelled = true
                    player.sendMessage(config.getString("chestProtection.messages.cannotPlaceAdjacent"))
                    return
                }
            }
        }

        // Check if placing a hopper below a protected chest
        else if (block.type == Material.HOPPER) {
            val above = block.getRelative(BlockFace.UP)
            if (above.type == Material.CHEST && isProtectedChest(above, player)) {
                event.isCancelled = true
                player.sendMessage(config.getString("chestProtection.messages.cannotPlaceHopper"))
                return
            }
        }

        // Prevent placing blocks that might interfere with shop chest access
        for (face in ADJACENT_FACES) {
            val adjacent = block.getRelative(face)
            if (adjacent.type == Material.CHEST && isProtectedChest(adjacent, player)) {
                // If the player is the owner, they can still place blocks near their shop
                if (!isShopOwner(adjacent, player)) {
                    event.isCancelled = true
                    player.sendMessage(config.getString("chestProtection.messages.cannotPlaceAdjacentBlock"))
                    return
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryMoveItem(event: InventoryMoveItemEvent) {
        val source = event.source
        val destination = event.destination

        // Check if items are being moved from a protected chest
        if (source.type == InventoryType.CHEST) {
            val holder = source.holder
            if (holder is Chest && isProtectedChest(holder.block, null)) {
                event.isCancelled = true
                return
            } else if (holder is DoubleChest) {
                val leftSide = holder.leftSide as? Chest
                val rightSide = holder.rightSide as? Chest

                if ((leftSide != null && isProtectedChest(leftSide.block, null)) ||
                    (rightSide != null && isProtectedChest(rightSide.block, null))) {
                    event.isCancelled = true
                    return
                }
            }
        }

        // Check if items are being moved to a protected chest
        if (destination.type == InventoryType.CHEST) {
            val holder = destination.holder
            if (holder is Chest && isProtectedChest(holder.block, null)) {
                event.isCancelled = true
                return
            } else if (holder is DoubleChest) {
                val leftSide = holder.leftSide as? Chest
                val rightSide = holder.rightSide as? Chest

                if ((leftSide != null && isProtectedChest(leftSide.block, null)) ||
                    (rightSide != null && isProtectedChest(rightSide.block, null))) {
                    event.isCancelled = true
                    return
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityExplode(event: EntityExplodeEvent) {
        val blocksToRemove = mutableListOf<Block>()

        for (block in event.blockList()) {
            if (block.type == Material.CHEST && isProtectedChest(block, null)) {
                blocksToRemove.add(block)
            } else {
                // Also protect blocks adjacent to shop chests
                for (face in ADJACENT_FACES) {
                    val adjacent = block.getRelative(face)
                    if (adjacent.type == Material.CHEST && isProtectedChest(adjacent, null)) {
                        blocksToRemove.add(block)
                        break
                    }
                }
            }
        }

        // Remove protected blocks from explosion list
        event.blockList().removeAll(blocksToRemove)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        val player = event.player

        // Check for redstone interaction with protected chests
        if (block.type == Material.CHEST && isProtectedChest(block, player)) {
            val item = event.item

            // Prevent using hoppers on protected chests
            if (item != null && (item.type == Material.HOPPER || item.type == Material.HOPPER_MINECART)) {
                event.isCancelled = true
                player.sendMessage(config.getString("chestProtection.messages.cannotUseHopper"))
                return
            }

            // Prevent using comparators, redstone, etc.
            if (item != null && (
                        item.type.name.contains("REDSTONE") ||
                                item.type == Material.COMPARATOR ||
                                item.type == Material.REPEATER ||
                                item.type == Material.OBSERVER
                        )) {
                event.isCancelled = true
                player.sendMessage(config.getString("chestProtection.messages.cannotUseRedstone"))
                return
            }
        }
    }

    // Helper method to check if a chest is a protected shop chest and the player is not the owner
    private fun isProtectedChest(block: Block, player: Player?): Boolean {
        if (block.type != Material.CHEST) return false

        val chestLocation = block.location.toString()
        val chestShop = iCottageSMultiPlugin.chestShops.find { it.chestLocation == chestLocation }

        // If no shop is associated with this chest, it's not protected
        if (chestShop == null) return false

        // If player is null, just return true as the chest is protected
        if (player == null) return true

        // If player is the owner, they can interact with their own shop
        return chestShop.owner != player.uniqueId
    }

    // Helper method to check if a player is the owner of a shop chest
    private fun isShopOwner(block: Block, player: Player): Boolean {
        if (block.type != Material.CHEST) return false

        val chestLocation = block.location.toString()
        val chestShop = iCottageSMultiPlugin.chestShops.find { it.chestLocation == chestLocation }

        // If no shop is associated with this chest, player is not the owner
        if (chestShop == null) return false

        return chestShop.owner == player.uniqueId
    }
}
