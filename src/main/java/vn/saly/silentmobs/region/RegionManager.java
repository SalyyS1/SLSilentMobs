package vn.saly.silentmobs.region;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import vn.saly.silentmobs.SLSilentMobs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all silent regions — CRUD, persistence, and spatial queries.
 * Now reads/writes from regions.yml instead of main config.yml.
 */
public class RegionManager {

    private final SLSilentMobs plugin;
    private final Map<String, SilentRegion> regions = new ConcurrentHashMap<>();

    public RegionManager(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Load all regions from regions.yml.
     */
    public void loadRegions() {
        regions.clear();
        FileConfiguration config = plugin.getConfigManager().getRegionsConfig();
        // Regions are stored at the root of regions.yml
        for (String name : config.getKeys(false)) {
            ConfigurationSection r = config.getConfigurationSection(name);
            if (r == null)
                continue;

            String world = r.getString("world", "world");
            int x1 = r.getInt("pos1.x"), y1 = r.getInt("pos1.y"), z1 = r.getInt("pos1.z");
            int x2 = r.getInt("pos2.x"), y2 = r.getInt("pos2.y"), z2 = r.getInt("pos2.z");

            SilentRegion region = new SilentRegion(name, world, x1, y1, z1, x2, y2, z2);

            // Load silent mobs
            for (String mob : r.getStringList("silent-mobs")) {
                region.addSilentMob(mob);
            }
            // Load exempt mobs
            for (String mob : r.getStringList("exempt-mobs")) {
                region.addExemptMob(mob);
            }
            // Load allowed players (stored as UUID strings)
            for (String uuid : r.getStringList("allowed-players")) {
                try {
                    region.addAllowedPlayer(UUID.fromString(uuid));
                } catch (IllegalArgumentException ignored) {
                }
            }
            // Load allowed permissions
            for (String perm : r.getStringList("allowed-permissions")) {
                region.addAllowedPermission(perm);
            }

            regions.put(name.toLowerCase(), region);
        }

        plugin.getLogger().info("Loaded " + regions.size() + " silent region(s)");
    }

    /**
     * Save all regions to regions.yml.
     */
    public void saveRegions() {
        FileConfiguration config = plugin.getConfigManager().getRegionsConfig();

        // Clear all existing keys
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        // Write each region at root level
        for (SilentRegion region : regions.values()) {
            String path = region.getName();
            config.set(path + ".world", region.getWorldName());
            config.set(path + ".pos1.x", region.getX1());
            config.set(path + ".pos1.y", region.getY1());
            config.set(path + ".pos1.z", region.getZ1());
            config.set(path + ".pos2.x", region.getX2());
            config.set(path + ".pos2.y", region.getY2());
            config.set(path + ".pos2.z", region.getZ2());
            config.set(path + ".silent-mobs", new ArrayList<>(region.getSilentMobs()));
            config.set(path + ".exempt-mobs", new ArrayList<>(region.getExemptMobs()));
            config.set(path + ".allowed-players",
                    region.getAllowedPlayers().stream().map(UUID::toString).collect(Collectors.toList()));
            config.set(path + ".allowed-permissions", new ArrayList<>(region.getAllowedPermissions()));
        }

        plugin.getConfigManager().saveRegionsConfig();
    }

    /**
     * Create a new region from two corner positions.
     */
    public SilentRegion createRegion(String name, Location pos1, Location pos2) {
        String key = name.toLowerCase();
        if (regions.containsKey(key))
            return null;

        SilentRegion region = new SilentRegion(name, pos1.getWorld().getName(), pos1, pos2);
        regions.put(key, region);
        saveRegions();
        return region;
    }

    /**
     * Delete a region by name.
     */
    public boolean deleteRegion(String name) {
        SilentRegion removed = regions.remove(name.toLowerCase());
        if (removed != null) {
            saveRegions();
            return true;
        }
        return false;
    }

    /**
     * Get a region by name.
     */
    public SilentRegion getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    /**
     * Get region(s) at a given location (a location can be in multiple regions).
     */
    public List<SilentRegion> getRegionsAt(Location loc) {
        List<SilentRegion> result = new ArrayList<>();
        for (SilentRegion region : regions.values()) {
            if (region.contains(loc))
                result.add(region);
        }
        return result;
    }

    /**
     * Get the first region at a location, or null.
     */
    public SilentRegion getFirstRegionAt(Location loc) {
        for (SilentRegion region : regions.values()) {
            if (region.contains(loc))
                return region;
        }
        return null;
    }

    /**
     * Get all regions.
     */
    public Collection<SilentRegion> getAllRegions() {
        return Collections.unmodifiableCollection(regions.values());
    }

    /**
     * Get region count.
     */
    public int getRegionCount() {
        return regions.size();
    }
}
