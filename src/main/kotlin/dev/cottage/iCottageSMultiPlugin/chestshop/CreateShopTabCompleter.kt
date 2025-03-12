package dev.cottage.iCottageSMultiPlugin.chestshop

import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.util.StringUtil
import java.util.*

class CreateShopTabCompleter : TabCompleter {
    // This method handles tab completion for the /createshop command
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        val completions = mutableListOf<String>()

        // First argument (sellItem)
        if (args.size == 1) {
            completions.addAll(Material.values().map { it.name.lowercase(Locale.getDefault()) })
        }
        // Second argument (buyItem)
        else if (args.size == 2) {
            completions.addAll(Material.values().map { it.name.lowercase(Locale.getDefault()) })
        }
        // Third and fourth arguments (sellAmount, buyAmount)
        else if (args.size == 3 || args.size == 4) {
            completions.addAll((1..64).map { it.toString() })
        }

        // Return filtered completions based on the user's input (partial matching)
        return StringUtil.copyPartialMatches(args.last(), completions, mutableListOf())
    }
}
