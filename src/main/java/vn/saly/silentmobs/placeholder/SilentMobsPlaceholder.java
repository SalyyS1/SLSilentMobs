package vn.saly.silentmobs.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.region.SilentRegion;

/**
 * PlaceholderAPI expansion for SLSilentMobs.
 * Provides placeholders: %slsilentmobs_<identifier>%
 */
public class SilentMobsPlaceholder extends PlaceholderExpansion {

    private final SLSilentMobs plugin;

    public SilentMobsPlaceholder(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "slsilentmobs";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SalyVn";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        // Server-wide placeholders (no player needed)
        switch (identifier.toLowerCase()) {
            case "count":
            case "total_count":
                return String.valueOf(plugin.getSilentMobManager().getTotalCount());
            case "global_status":
                return plugin.getGlobalSilentManager().isEnabled() ? "ON" : "OFF";
            case "region_count":
                return String.valueOf(plugin.getRegionManager().getRegionCount());
        }

        // Player-specific placeholders
        if (player == null)
            return "";

        switch (identifier.toLowerCase()) {
            case "player_count":
                return String.valueOf(plugin.getSilentMobManager().getActiveCount(player.getUniqueId()));
            case "player_max":
                return String.valueOf(plugin.getConfig().getInt("settings.max-mobs-per-player", 10));
            case "player_in_region": {
                SilentRegion r = plugin.getRegionManager().getFirstRegionAt(player.getLocation());
                return r != null ? r.getName() : "none";
            }
            case "player_visible_mobs":
                return String.valueOf(plugin.getSilentMobManager().getActiveCount(player.getUniqueId()));
        }

        // Dynamic region placeholders: region_<name>_mobs, region_<name>_players
        if (identifier.startsWith("region_")) {
            String rest = identifier.substring(7); // remove "region_"

            // Find last underscore to split name from property
            int lastUnderscore = rest.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String regionName = rest.substring(0, lastUnderscore);
                String property = rest.substring(lastUnderscore + 1);
                SilentRegion region = plugin.getRegionManager().getRegion(regionName);
                if (region != null) {
                    switch (property) {
                        case "mobs":
                            return String.valueOf(region.getSilentMobs().size());
                        case "players":
                            return String.valueOf(region.getAllowedPlayers().size());
                        case "perms":
                            return String.valueOf(region.getAllowedPermissions().size());
                    }
                }
            }

            // player_can_see_<region>
            if (identifier.startsWith("player_can_see_") && player != null) {
                String regionName = identifier.substring(15);
                SilentRegion region = plugin.getRegionManager().getRegion(regionName);
                if (region != null) {
                    return region.canPlayerSee(player) ? "true" : "false";
                }
                return "false";
            }
        }

        return null;
    }
}
