package dev.cottage.iCottageSMultiPlugin.tpa

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import java.util.*
import kotlin.collections.ArrayList

// Storing the pending TPA requests - maps target UUID to (requester UUID, timestamp)
class TPAManager {
    val pendingRequests = mutableMapOf<UUID, Pair<UUID, Long>>()
    val teleportDelay: Long
    val maxDistance: Int
    val crossDimensional: Boolean

    constructor(plugin: iCottageSMultiPlugin) {
        teleportDelay = plugin.config.getLong("TPA.teleport-delay", 5) * 1000
        maxDistance = plugin.TPAMaxDistance
        crossDimensional = plugin.TPACrossDimensional
    }

    // Checks if a teleport request is allowed based on configuration
    fun isAllowed(requester: Player, target: Player): Boolean {
        // Check for cross-dimensional teleporting
        if (!crossDimensional && requester.world.uid != target.world.uid) {
            return false
        }

        // Check for maximum distance
        if (maxDistance > 0 && requester.world.uid == target.world.uid) {
            val distance = requester.location.distance(target.location)
            if (distance > maxDistance) {
                return false
            }
        }

        return true
    }
}

class TPACommand : CommandExecutor, TabCompleter {
    private val plugin = iCottageSMultiPlugin.instance
    private val tpaManager: TPAManager

    init {
        tpaManager = TPAManager(plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cUsage: /tpa <player>")
            return true
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null || !target.isOnline) {
            sender.sendMessage("§cPlayer not found or offline.")
            return true
        }

        if (sender.uniqueId == target.uniqueId) {
            sender.sendMessage("§cYou cannot send a teleport request to yourself.")
            return true
        }

        // Check if teleport is allowed based on configuration
        if (!tpaManager.isAllowed(sender, target)) {
            if (!tpaManager.crossDimensional && sender.world.uid != target.world.uid) {
                sender.sendMessage("§cYou cannot teleport between dimensions.")
            } else if (tpaManager.maxDistance > 0) {
                sender.sendMessage("§cTarget is too far away (max distance: ${tpaManager.maxDistance} blocks).")
            }
            return true
        }

        tpaManager.pendingRequests[target.uniqueId] = sender.uniqueId to System.currentTimeMillis()
        sender.sendMessage("§aTeleport request sent to §f${target.name}§a.")
        target.sendMessage("§f${sender.name} §awants to teleport to you. Type §f/tpaccept §ato allow or §f/tpdeny §ato deny.")

        // Expire the request after 60 seconds
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val currentRequest = tpaManager.pendingRequests[target.uniqueId]
            if (currentRequest != null && currentRequest.first == sender.uniqueId) {
                tpaManager.pendingRequests.remove(target.uniqueId)
                sender.sendMessage("§cYour teleport request to §f${target.name} §chas expired.")
                target.sendMessage("§cTeleport request from §f${sender.name} §chas expired.")
            }
        }, 1200) // 60 seconds in ticks

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val completions = ArrayList<String>()
            val partialName = args[0].lowercase()

            for (player in Bukkit.getOnlinePlayers()) {
                if (player.name.lowercase().startsWith(partialName) && player.uniqueId != (sender as? Player)?.uniqueId) {
                    completions.add(player.name)
                }
            }

            return completions
        }
        return emptyList()
    }
}

class TPAcceptCommand : CommandExecutor {
    private val plugin = iCottageSMultiPlugin.instance
    private val tpaManager: TPAManager

    init {
        tpaManager = TPAManager(plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command.")
            return true
        }

        val request = tpaManager.pendingRequests[sender.uniqueId]
        if (request == null) {
            sender.sendMessage("§cNo pending teleport requests.")
            return true
        }

        val (requesterId, requestTime) = request
        val requester = Bukkit.getPlayer(requesterId)
        if (requester == null || !requester.isOnline) {
            sender.sendMessage("§cRequester is no longer online.")
            tpaManager.pendingRequests.remove(sender.uniqueId)
            return true
        }

        val teleportDelay = tpaManager.teleportDelay / 1000
        sender.sendMessage("§aTeleporting §f${requester.name} §ain §f$teleportDelay §aseconds...")
        requester.sendMessage("§aTeleporting to §f${sender.name} §ain §f$teleportDelay §aseconds...")

        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            requester.teleport(sender.location)
            sender.sendMessage("§f${requester.name} §ahas been teleported to you.")
            requester.sendMessage("§aTeleported to §f${sender.name}§a!")
        }, tpaManager.teleportDelay / 50) // Convert ms to ticks

        tpaManager.pendingRequests.remove(sender.uniqueId)
        return true
    }
}

class TPDenyCommand : CommandExecutor {
    private val tpaManager: TPAManager

    init {
        tpaManager = TPAManager(iCottageSMultiPlugin.instance)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§cOnly players can use this command.")
            return true
        }

        val request = tpaManager.pendingRequests[sender.uniqueId]
        if (request == null) {
            sender.sendMessage("§cNo pending teleport requests.")
            return true
        }

        val (requesterId, _) = request
        val requester = Bukkit.getPlayer(requesterId)
        if (requester != null && requester.isOnline) {
            requester.sendMessage("§f${sender.name} §chas denied your teleport request.")
        }

        sender.sendMessage("§cTeleport request denied.")
        tpaManager.pendingRequests.remove(sender.uniqueId)
        return true
    }
}
