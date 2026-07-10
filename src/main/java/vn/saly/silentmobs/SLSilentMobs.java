package vn.saly.silentmobs;

import org.bukkit.plugin.java.JavaPlugin;
import vn.saly.silentmobs.command.SilentMobCommand;
import vn.saly.silentmobs.command.SilentMobTab;
import vn.saly.silentmobs.config.ConfigManager;
import vn.saly.silentmobs.global.GlobalSilentConfig;
import vn.saly.silentmobs.global.GlobalSilentManager;
import vn.saly.silentmobs.listener.PlayerConnectionListener;
import vn.saly.silentmobs.listener.SilentMobDeathListener;
import vn.saly.silentmobs.listener.SilentMobDamageListener;
import vn.saly.silentmobs.listener.SilentMobTargetListener;
import vn.saly.silentmobs.loot.SilentDropListener;
import vn.saly.silentmobs.loot.SilentDropManager;
import vn.saly.silentmobs.manager.SilentMobManager;
import vn.saly.silentmobs.placeholder.SilentMobsPlaceholder;
import vn.saly.silentmobs.region.RegionManager;
import vn.saly.silentmobs.region.RegionSilentListener;
import vn.saly.silentmobs.region.RegionSpawnListener;
import vn.saly.silentmobs.region.WandListener;
import vn.saly.silentmobs.region.WandManager;
import vn.saly.silentmobs.task.MobTimeoutTask;
import vn.saly.silentmobs.visibility.EntityHider;

/**
 * Private/Silent Mobs & Instanced Loot for RPG Servers.
 * Author: SalyVn
 *
 * Features:
 * - Manual Silent Mobs: spawn mobs visible only to a specific player or
 * permission
 * - Global Silent Mode: ALL mobs become private (Wynncraft-style)
 * - Silent Item Drops: per-player instanced loot
 * - Silent Regions: area-based mob visibility with wand tool
 * - PlaceholderAPI integration
 * - Multi-language support (EN/VI)
 */
public class SLSilentMobs extends JavaPlugin {

    private ConfigManager configManager;
    private EntityHider entityHider;
    private SilentMobManager silentMobManager;
    private GlobalSilentConfig globalSilentConfig;
    private GlobalSilentManager globalSilentManager;
    private SilentDropManager silentDropManager;
    private RegionManager regionManager;
    private WandManager wandManager;

    @Override
    public void onEnable() {
        // Initialize config manager (handles all config files)
        configManager = new ConfigManager(this);
        configManager.setup();

        // Initialize EntityHider (ProtocolLib packet-level visibility)
        entityHider = new EntityHider(this);

        // Initialize managers
        silentMobManager = new SilentMobManager(this, entityHider);
        silentDropManager = new SilentDropManager(this);

        // Region system
        wandManager = new WandManager(this);
        regionManager = new RegionManager(this);
        regionManager.loadRegions();

        // Global Silent config
        globalSilentConfig = new GlobalSilentConfig();
        globalSilentConfig.load(configManager.getConfig());

        // Global Silent manager
        globalSilentManager = new GlobalSilentManager(this);
        getServer().getPluginManager().registerEvents(globalSilentManager, this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new SilentMobDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new SilentMobTargetListener(this), this);
        getServer().getPluginManager().registerEvents(new SilentMobDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SilentDropListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionSilentListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionSpawnListener(this), this);

        // Register command
        var cmd = getCommand("silentmob");
        if (cmd != null) {
            cmd.setExecutor(new SilentMobCommand(this));
            cmd.setTabCompleter(new SilentMobTab(this));
        }

        // Start timeout cleanup task (every 5 seconds = 100 ticks)
        new MobTimeoutTask(this).runTaskTimer(this, 100L, 100L);

        // PlaceholderAPI hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new SilentMobsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hooked — placeholders registered");
        }

        if (entityHider.isModelEngineAvailable()) {
            getLogger().info("ModelEngine " + entityHider.getModelEngineVersion()
                    + " hooked - client-side model visibility enabled");
        }

        getLogger().info("SLSilentMobs v" + getDescription().getVersion() + " enabled | SalyVn");
        getLogger().info("Language: " + configManager.getLanguage().toUpperCase());
        getLogger().info("Global Silent: " + (globalSilentManager.isEnabled() ? "ON" : "OFF"));
        getLogger().info("Regions loaded: " + regionManager.getRegionCount());
    }

    @Override
    public void onDisable() {
        // Save regions
        if (regionManager != null) {
            regionManager.saveRegions();
        }

        // Despawn all silent mobs
        if (silentMobManager != null) {
            int count = silentMobManager.despawnAll();
            getLogger().info("Despawned " + count + " silent mob(s)");
        }

        // Cleanup silent drops
        if (silentDropManager != null) {
            silentDropManager.clearAll();
        }

        // Close EntityHider (remove packet listeners)
        if (entityHider != null) {
            entityHider.close();
        }

        getLogger().info("SLSilentMobs disabled");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public EntityHider getEntityHider() {
        return entityHider;
    }

    public SilentMobManager getSilentMobManager() {
        return silentMobManager;
    }

    public GlobalSilentConfig getGlobalSilentConfig() {
        return globalSilentConfig;
    }

    public GlobalSilentManager getGlobalSilentManager() {
        return globalSilentManager;
    }

    public SilentDropManager getSilentDropManager() {
        return silentDropManager;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }

    public WandManager getWandManager() {
        return wandManager;
    }
}
