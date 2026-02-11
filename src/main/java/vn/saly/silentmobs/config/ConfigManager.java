package vn.saly.silentmobs.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import vn.saly.silentmobs.SLSilentMobs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages all configuration files for SLSilentMobs.
 * Splits config into: config.yml, drops.yml, regions.yml, messages.yml
 */
public class ConfigManager {

    private final SLSilentMobs plugin;

    private FileConfiguration dropsConfig;
    private FileConfiguration regionsConfig;
    private FileConfiguration messagesConfig;

    private File dropsFile;
    private File regionsFile;
    private File messagesFile;

    private String language;

    public ConfigManager(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize all config files — called once on plugin enable.
     */
    public void setup() {
        // Main config.yml (handled by Bukkit)
        plugin.saveDefaultConfig();

        // Custom config files
        dropsFile = new File(plugin.getDataFolder(), "drops.yml");
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        saveDefaultIfMissing(dropsFile, "drops.yml");
        saveDefaultIfMissing(regionsFile, "regions.yml");
        saveDefaultIfMissing(messagesFile, "messages.yml");

        reloadAll();
    }

    /**
     * Reload all configuration files from disk.
     */
    public void reloadAll() {
        // Main config
        plugin.reloadConfig();

        // Drops
        dropsConfig = YamlConfiguration.loadConfiguration(dropsFile);
        mergeDefaults(dropsConfig, "drops.yml");

        // Regions
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);

        // Messages
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        mergeDefaults(messagesConfig, "messages.yml");

        // Cache language
        language = messagesConfig.getString("language", "en").toLowerCase();
    }

    // ==========================================
    // Config getters
    // ==========================================

    /**
     * Main config.yml (settings + global-silent).
     */
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    /**
     * drops.yml (silent-drops settings).
     */
    public FileConfiguration getDropsConfig() {
        return dropsConfig;
    }

    /**
     * regions.yml (region definitions).
     */
    public FileConfiguration getRegionsConfig() {
        return regionsConfig;
    }

    /**
     * messages.yml (all messages + language).
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Current language code (en/vi).
     */
    public String getLanguage() {
        return language;
    }

    // ==========================================
    // Message helpers
    // ==========================================

    /**
     * Get a localized message by key using current language.
     * Falls back to "en" if key not found in current language.
     * Falls back to the key itself if not found at all.
     */
    public String getMessage(String key) {
        String msg = messagesConfig.getString(language + "." + key);
        if (msg == null) {
            msg = messagesConfig.getString("en." + key);
        }
        if (msg == null) {
            msg = "&cMissing message: " + key;
        }
        return msg;
    }

    /**
     * Get a localized message with color codes translated.
     */
    public String getColoredMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', getMessage(key));
    }

    /**
     * Get the message prefix with color codes.
     */
    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', getMessage("prefix"));
    }

    // ==========================================
    // Save helpers
    // ==========================================

    /**
     * Save regions.yml to disk.
     */
    public void saveRegionsConfig() {
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save regions.yml: " + e.getMessage());
        }
    }

    /**
     * Save drops.yml to disk.
     */
    public void saveDropsConfig() {
        try {
            dropsConfig.save(dropsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save drops.yml: " + e.getMessage());
        }
    }

    /**
     * Save messages.yml to disk.
     */
    public void saveMessagesConfig() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml: " + e.getMessage());
        }
    }

    // ==========================================
    // Internal helpers
    // ==========================================

    private void saveDefaultIfMissing(File file, String resourceName) {
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
        }
    }

    private void mergeDefaults(FileConfiguration config, String resourceName) {
        InputStream stream = plugin.getResource(resourceName);
        if (stream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            config.setDefaults(defaults);
        }
    }
}
