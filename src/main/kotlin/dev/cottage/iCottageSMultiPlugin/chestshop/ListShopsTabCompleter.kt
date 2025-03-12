package dev.cottage.iCottageSMultiPlugin.chestshop

import dev.cottage.iCottageSMultiPlugin.iCottageSMultiPlugin
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.util.StringUtil
import java.util.*

class ListShopsTabCompleter : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String> {
        val completions = mutableListOf<String>()

        // First argument - sort type
        if (args.size == 1) {
            completions.add("proximity")
            completions.add("item")
            return StringUtil.copyPartialMatches(args[0], completions, mutableListOf())
        }

        // Second argument - item name (only if first arg is "item")
        if (args.size == 2 && args[0].equals("item", ignoreCase = true)) {
            // Provide all valid in-game items
            for (material in Material.values()) {
                if (material.isItem && !material.isAir) {
                    completions.add(material.name.lowercase(Locale.getDefault()))
                }
            }
            return StringUtil.copyPartialMatches(args[1], completions, mutableListOf())
        }

        return mutableListOf()
    }
}
