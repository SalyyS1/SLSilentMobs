package vn.saly.silentmobs.global;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration for Global Silent mode whitelist.
 */
public class GlobalSilentConfig {

    private final Set<String> vanillaWhitelist = new HashSet<>();
    private final Set<String> mythicWhitelist = new HashSet<>();

    public void load(FileConfiguration config) {
        vanillaWhitelist.clear();
        mythicWhitelist.clear();

        List<String> vanilla = config.getStringList("global-silent.whitelist.vanilla");
        for (String s : vanilla) {
            vanillaWhitelist.add(s.toUpperCase());
        }

        List<String> mythic = config.getStringList("global-silent.whitelist.mythicmobs");
        for (String s : mythic) {
            mythicWhitelist.add(s);
        }
    }

    /**
     * Check if a vanilla EntityType is whitelisted (should NOT be hidden).
     */
    public boolean isWhitelistedVanilla(EntityType type) {
        return vanillaWhitelist.contains(type.name());
    }

    /**
     * Check if a MythicMob ID is whitelisted.
     */
    public boolean isWhitelistedMythic(String mobId) {
        return mythicWhitelist.contains(mobId);
    }

    /**
     * Check by string (either vanilla or mythic).
     */
    public boolean isWhitelisted(String mobId) {
        return vanillaWhitelist.contains(mobId.toUpperCase()) || mythicWhitelist.contains(mobId);
    }

    /**
     * Add to whitelist. Returns false if already exists.
     */
    public boolean addToWhitelist(String mobId) {
        // Try as vanilla first
        try {
            EntityType.valueOf(mobId.toUpperCase());
            return vanillaWhitelist.add(mobId.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Treat as MythicMob
            return mythicWhitelist.add(mobId);
        }
    }

    /**
     * Remove from whitelist. Returns false if not found.
     */
    public boolean removeFromWhitelist(String mobId) {
        boolean removed = vanillaWhitelist.remove(mobId.toUpperCase());
        if (!removed) {
            removed = mythicWhitelist.remove(mobId);
        }
        return removed;
    }

    /**
     * Save whitelist back to config.
     */
    public void save(FileConfiguration config) {
        config.set("global-silent.whitelist.vanilla", new ArrayList<>(vanillaWhitelist));
        config.set("global-silent.whitelist.mythicmobs", new ArrayList<>(mythicWhitelist));
    }

    public Set<String> getVanillaWhitelist() {
        return vanillaWhitelist;
    }

    public Set<String> getMythicWhitelist() {
        return mythicWhitelist;
    }
}
