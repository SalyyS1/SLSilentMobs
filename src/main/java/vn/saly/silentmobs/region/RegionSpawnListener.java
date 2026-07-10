package vn.saly.silentmobs.region;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.model.SilentMob;
import vn.saly.silentmobs.util.MobSpawner;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Spawns private mobs when players enter configured regions.
 */
public class RegionSpawnListener implements Listener {

    private final SLSilentMobs plugin;
    private final Map<CooldownKey, Long> nextSpawnAt = new ConcurrentHashMap<>();

    public RegionSpawnListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || isSameBlock(from, to)) {
            return;
        }

        for (SilentRegion region : plugin.getRegionManager().getRegionsAt(to)) {
            if (!region.contains(from)) {
                triggerRegion(event.getPlayer(), region);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (SilentRegion region : plugin.getRegionManager().getRegionsAt(player.getLocation())) {
                triggerRegion(player, region);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        nextSpawnAt.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private void triggerRegion(Player player, SilentRegion region) {
        long now = System.currentTimeMillis();
        nextSpawnAt.entrySet().removeIf(entry -> entry.getValue() <= now);

        if (region.getSpawnEntries().isEmpty()) {
            return;
        }
        if (region.hasAccessRules() && !region.canPlayerSee(player)) {
            return;
        }

        int maxPerPlayer = plugin.getConfigManager().getConfig().getInt("settings.max-mobs-per-player", 10);
        int activeCount = plugin.getSilentMobManager().getActiveCount(player.getUniqueId());

        for (RegionSpawnEntry entry : region.getSpawnEntries()) {
            if (activeCount >= maxPerPlayer || !canSpawn(player.getUniqueId(), region, entry)) {
                continue;
            }

            int amount = Math.min(entry.getAmount(), maxPerPlayer - activeCount);
            int spawned = spawnForPlayer(player, region, entry, amount);
            if (spawned > 0) {
                activeCount += spawned;
                setCooldown(player.getUniqueId(), region, entry);
            }
        }
    }

    private int spawnForPlayer(Player player, SilentRegion region, RegionSpawnEntry entry, int amount) {
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            Location spawnLoc = findSpawnLocation(player.getLocation(), region, entry.getSpread());
            Entity entity = MobSpawner.spawn(entry.getMobId(), spawnLoc, entry.getLevel());
            if (entity == null) {
                continue;
            }

            SilentMob silentMob = new SilentMob(player, entry.getMobId(), spawnLoc, entry.getLevel(), false);
            silentMob.setEntity(entity);
            silentMob.setRegionName(region.getName());
            plugin.getSilentMobManager().addSilentMob(silentMob);
            spawned++;
        }
        return spawned;
    }

    private Location findSpawnLocation(Location base, SilentRegion region, double spread) {
        if (spread <= 0) {
            return base.clone();
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 8; i++) {
            double x = base.getX() + random.nextDouble(-spread, spread);
            double z = base.getZ() + random.nextDouble(-spread, spread);
            Location candidate = new Location(base.getWorld(), x, base.getY(), z, base.getYaw(), base.getPitch());
            if (region.contains(candidate)) {
                return candidate;
            }
        }
        return base.clone();
    }

    private boolean canSpawn(UUID playerUuid, SilentRegion region, RegionSpawnEntry entry) {
        long next = nextSpawnAt.getOrDefault(cooldownKey(playerUuid, region, entry), 0L);
        return System.currentTimeMillis() >= next;
    }

    private void setCooldown(UUID playerUuid, SilentRegion region, RegionSpawnEntry entry) {
        if (entry.getCooldownSeconds() <= 0) {
            return;
        }
        nextSpawnAt.put(cooldownKey(playerUuid, region, entry),
                System.currentTimeMillis() + entry.getCooldownSeconds() * 1000L);
    }

    private CooldownKey cooldownKey(UUID playerUuid, SilentRegion region, RegionSpawnEntry entry) {
        return new CooldownKey(playerUuid, region.getName(), entry.getMobId());
    }

    private boolean isSameBlock(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null || !a.getWorld().equals(b.getWorld())) {
            return false;
        }
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private record CooldownKey(UUID playerId, String regionName, String mobId) {
        private CooldownKey {
            regionName = regionName.toLowerCase(java.util.Locale.ROOT);
            mobId = mobId.toLowerCase(java.util.Locale.ROOT);
        }
    }
}
