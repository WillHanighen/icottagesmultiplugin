package dev.cottage.iCottageSMultiPlugin

import ChestShop
import dev.cottage.iCottageSMultiPlugin.chestshop.ChestShopListener
import dev.cottage.iCottageSMultiPlugin.chestshop.CreateShopCommand
import dev.cottage.iCottageSMultiPlugin.chestshop.RemoveShopCommand
import dev.cottage.iCottageSMultiPlugin.chestshop.ListShopsCommand
import dev.cottage.iCottageSMultiPlugin.chestshop.ListShopsTabCompleter
import dev.cottage.iCottageSMultiPlugin.chestshop.CreateShopTabCompleter
import dev.cottage.iCottageSMultiPlugin.chestshop.ChestProtectionListener
import dev.cottage.iCottageSMultiPlugin.tpa.TPACommand
import dev.cottage.iCottageSMultiPlugin.tpa.TPAcceptCommand
import dev.cottage.iCottageSMultiPlugin.tpa.TPDenyCommand
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.reflect.Type
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.Bukkit
import org.bukkit.ChatColor

class iCottageSMultiPlugin : JavaPlugin() {

    private lateinit var config: FileConfiguration
    private var debugMode: Boolean = false

    // Feature toggle flags
    private var chestShopEnabled: Boolean = true
    private var chestProtectionEnabled: Boolean = true
    private var TPAEnabled: Boolean = true
    var TPAMaxDistance: Int = -1
    var TPACrossDimensional: Boolean = true
    var TPATeleportDelay: Long = 5 // Default to 5 seconds

    companion object {
        lateinit var instance: iCottageSMultiPlugin
        val chestShops = mutableListOf<ChestShop>()

        // Add protection settings
        var protectAdjacentBlocks: Boolean = true
        var preventHopperInteraction: Boolean = true
        var preventExplosionDamage: Boolean = true
    }

    override fun onEnable() {
        // Set instance for global access
        instance = this
        saveDefaultConfig()

        // Load configuration
        loadConfig()

        // Create data folder if it doesn't exist
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }

        // Initialize features based on configuration
        initializeFeatures()

        logger.info("iCottageSMultiPlugin enabled!")

        if (debugMode) {
            logger.info("Debug mode enabled")
            logger.info("Features enabled:")
            logger.info("- Chest Shop: $chestShopEnabled")
            logger.info("- Chest Protection: $chestProtectionEnabled")
            logger.info("- TPA: $TPAEnabled")

            if (chestProtectionEnabled) {
                logger.info("Protection settings:")
                logger.info("- Protect adjacent blocks: $protectAdjacentBlocks")
                logger.info("- Prevent hopper interaction: $preventHopperInteraction")
                logger.info("- Prevent explosion damage: $preventExplosionDamage")
            }

            if (chestShopEnabled) {
                logger.info("Loaded ${chestShops.size} chest shops")
            }

            if (TPAEnabled) {
                logger.info("TPA settings")
                logger.info("- maximum distance: $TPAMaxDistance")
                logger.info("- Cross-dimensional: $TPACrossDimensional")
                logger.info("- Teleport delay: $TPATeleportDelay seconds")
            }
        }
    }

    override fun onDisable() {
        // Save chest shops to file/database if the feature is enabled
        if (chestShopEnabled) {
            saveChestShops()
        }
        logger.info("iCottageSMultiPlugin disabled!")
    }

    private fun loadConfig() {
        // Save default config if it doesn't exist
        saveDefaultConfig()

        // Load config
        reloadConfig()
        config = getConfig()

        // Load general settings
        debugMode = config.getBoolean("debug-mode", false)

        // Load feature toggles
        chestShopEnabled = config.getBoolean("chestShop.enabled", true)
        chestProtectionEnabled = config.getBoolean("chestProtection.enabled", true)
        TPAEnabled = config.getBoolean("TPA.enabled", true)

        // Load protection settings if protection is enabled
        if (chestProtectionEnabled) {
            protectAdjacentBlocks = config.getBoolean("protection.adjacent-blocks", true)
            preventHopperInteraction = config.getBoolean("protection.hopper-interaction", true)
            preventExplosionDamage = config.getBoolean("protection.explosion-damage", true)
        }

        // Load TPA settings
        if (TPAEnabled) {
            TPAMaxDistance = config.getInt("TPA.max-distance", -1)
            TPACrossDimensional = config.getBoolean("TPA.cross-dimensional", true)
            TPATeleportDelay = config.getLong("TPA.teleport-delay", 5)
        }

        // Create default config if it doesn't exist
        if (!File(dataFolder, "config.yml").exists()) {
            config.set("debug-mode", false)
            config.set("chestShop.enabled", true)
            config.set("chestProtection.enabled", true)
            config.set("protection.adjacent-blocks", true)
            config.set("protection.hopper-interaction", true)
            config.set("protection.explosion-damage", true)
            config.set("TPA.enabled", true)
            config.set("TPA.max-distance", -1)
            config.set("TPA.cross-dimensional", true)
            config.set("TPA.teleport-delay", 5)
            saveConfig()
        }
    }

    private fun initializeFeatures() {
        val plugin = this

        // Initialize chest shop feature if enabled
        if (chestShopEnabled) {
            debug("Initializing chest shop feature")

            // Register chest shop listener
            server.pluginManager.registerEvents(ChestShopListener(), this)

            // Register chest shop commands
            getCommand("createshop")?.setExecutor(CreateShopCommand())
            getCommand("createshop")?.tabCompleter = CreateShopTabCompleter()
            getCommand("removeshop")?.setExecutor(RemoveShopCommand())
            getCommand("shops")?.setExecutor(ListShopsCommand())
            getCommand("shops")?.tabCompleter = ListShopsTabCompleter()

            // Load chest shops from file/database
            loadChestShops()
        } else {
            debug("Chest shop feature is disabled")
        }

        // Initialize chest protection feature if enabled
        if (chestProtectionEnabled) {
            debug("Initializing chest protection feature")

            // Register chest protection listener
            server.pluginManager.registerEvents(ChestProtectionListener(), this)
        } else {
            debug("Chest protection feature is disabled")
        }

        // Initialize TPA feature if enabled
        if (TPAEnabled) {
            debug("Initializing TPA feature")

            // Register TPA commands
            getCommand("tpa")?.setExecutor(TPACommand())
            getCommand("tpa")?.tabCompleter = TPACommand()
            getCommand("tpaccept")?.setExecutor(TPAcceptCommand())
            getCommand("tpdeny")?.setExecutor(TPDenyCommand())
        } else {
            debug("TPA feature is disabled")
        }
    }

    // Method to save chest shops
    fun saveChestShops() {
        try {
            if (debugMode) {
                logger.info("Saving chest shops: ${chestShops.size}")
            }

            val serializableShops = chestShops.map { shop ->
                mapOf(
                    "chestLocation" to shop.chestLocation,
                    "sellItem" to shop.sellItem.serialize(),  // Convert ItemStack to Map
                    "buyItem" to shop.buyItem.serialize(),    // Convert ItemStack to Map
                    "sellAmount" to shop.sellAmount,
                    "buyAmount" to shop.buyAmount,
                    "owner" to shop.owner.toString()
                )
            }

            val file = File(dataFolder, "chestShops.json")
            file.writeText(Gson().toJson(serializableShops))

            if (debugMode) {
                logger.info("Chest shops saved successfully.")
            }
        } catch (e: Exception) {
            logger.severe("Error saving chest shops: ${e.message}")
            e.printStackTrace()
        }
    }

    // Method to load chest shops
    fun loadChestShops() {
        // yippi
        try {
            val file = File(dataFolder, "chestShops.json")
            if (!file.exists()) return

            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val shopList: List<Map<String, Any>> = Gson().fromJson(file.readText(), type)

            chestShops.clear()
            chestShops.addAll(shopList.map { map ->
                val shop = ChestShop(
                    chestLocation = map["chestLocation"] as String,
                    sellItem = ItemStack.deserialize(map["sellItem"] as Map<String, Any>),  // Deserialize ItemStack
                    buyItem = ItemStack.deserialize(map["buyItem"] as Map<String, Any>),    // Deserialize ItemStack
                    sellAmount = (map["sellAmount"] as Number).toInt(),
                    buyAmount = (map["buyAmount"] as Number).toInt(),
                    owner = UUID.fromString(map["owner"] as String)
                )

                shop
            })

            if (debugMode) {
                logger.info("Chest shops loaded successfully: ${chestShops.size} shops")
            }
        } catch (e: Exception) {
            logger.severe("Error loading chest shops: ${e.message}")
            e.printStackTrace()
        }
    }

    // Utility function to check if a chest shop exists at a location
    fun hasShopAt(chestLocation: String): Boolean {
        if (!chestShopEnabled) return false
        return chestShops.any { it.chestLocation == chestLocation }
    }

    // Utility function to get a shop at a location
    fun getShopAt(chestLocation: String): ChestShop? {
        if (!chestShopEnabled) return null
        return chestShops.find { it.chestLocation == chestLocation }
    }

    // Send a debug message if debug mode is enabled
    fun debug(message: String) {
        if (debugMode) {
            logger.info("[DEBUG] $message")
        }
    }

    // Send an admin message to players with permission
    fun sendAdminMessage(message: String) {
        val formattedMessage = ChatColor.GOLD.toString() + "[iCottageS] " + ChatColor.WHITE + message

        Bukkit.getOnlinePlayers().forEach { player ->
            if (player.hasPermission("icottages.admin.notifications")) {
                player.sendMessage(formattedMessage)
            }
        }

        logger.info(ChatColor.stripColor(formattedMessage))
    }

    // Helper methods to check if features are enabled
    fun isChestShopEnabled(): Boolean {
        return chestShopEnabled
    }

    fun isChestProtectionEnabled(): Boolean {
        return chestProtectionEnabled
    }

    fun isTPAEnabled(): Boolean {
        return TPAEnabled
    }
}
