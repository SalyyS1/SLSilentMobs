package vn.saly.silentmobs.command;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.config.ConfigManager;
import vn.saly.silentmobs.model.SilentMob;
import vn.saly.silentmobs.region.SilentRegion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main command handler for /silentmob (/sm).
 */
public class SilentMobCommand implements CommandExecutor {

    private final SLSilentMobs plugin;

    public SilentMobCommand(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("silentmob.use")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return true;
        }
        if (args.length == 0) {
            handleHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "despawn" -> handleDespawn(sender, args);
            case "despawnall" -> handleDespawnAll(sender);
            case "list" -> handleList(sender, args);
            case "global" -> handleGlobal(sender, args);
            case "reload" -> handleReload(sender);
            case "wand" -> handleWand(sender);
            case "region" -> handleRegion(sender, args);
            case "help" -> handleHelp(sender);
            default -> handleHelp(sender);
        }
        return true;
    }

    // ==========================================
    // /sm help
    // ==========================================
    private void handleHelp(CommandSender sender) {
        ConfigManager cm = plugin.getConfigManager();
        String version = plugin.getDescription().getVersion();

        sendMsg(sender, cm.getMessage("help-header"));
        sendMsg(sender, cm.getMessage("help-version").replace("{version}", version));
        sendMsg(sender, cm.getMessage("help-line"));
        sendMsg(sender, cm.getMessage("help-spawn"));
        sendMsg(sender, cm.getMessage("help-spawn-desc"));
        sendMsg(sender, cm.getMessage("help-despawn"));
        sendMsg(sender, cm.getMessage("help-despawn-desc"));
        sendMsg(sender, cm.getMessage("help-despawnall"));
        sendMsg(sender, cm.getMessage("help-despawnall-desc"));
        sendMsg(sender, cm.getMessage("help-list"));
        sendMsg(sender, cm.getMessage("help-list-desc"));
        sendMsg(sender, cm.getMessage("help-global"));
        sendMsg(sender, cm.getMessage("help-global-desc"));
        sendMsg(sender, cm.getMessage("help-reload"));
        sendMsg(sender, cm.getMessage("help-reload-desc"));
        sendMsg(sender, cm.getMessage("help-wand"));
        sendMsg(sender, cm.getMessage("help-wand-desc"));
        sendMsg(sender, cm.getMessage("help-region"));
        sendMsg(sender, cm.getMessage("help-region-desc"));
        sendMsg(sender, cm.getMessage("help-footer"));
        sendMsg(sender, cm.getMessage("help-tip"));
    }

    // ==========================================
    // /silentmob spawn <player> <mob> [amount] [level] [world] [x] [y] [z] [-p
    // <permission>]
    // ==========================================
    private void handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender,
                    "&eUsage: &f/sm spawn <player> <mob> [amount] [level] [world] [x] [y] [z] [-p <permission>]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, cfgMsg("player-offline").replace("{player}", args[1]));
            return;
        }

        String mobId = args[2];
        int amount = 1;
        int level = 1;
        Location loc = target.getLocation();
        String viewPermission = null;

        // Parse optional args, looking for -p flag
        int i = 3;
        if (i < args.length && !args[i].equals("-p")) {
            try {
                amount = Integer.parseInt(args[i]);
                i++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (i < args.length && !args[i].equals("-p")) {
            try {
                level = Integer.parseInt(args[i]);
                i++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (i < args.length && !args[i].equals("-p")) {
            World world = Bukkit.getWorld(args[i]);
            if (world == null) {
                sendMsg(sender, cfgMsg("world-invalid").replace("{world}", args[i]));
                return;
            }
            i++;
            if (i + 2 < args.length) {
                try {
                    double x = Double.parseDouble(args[i]);
                    double y = Double.parseDouble(args[i + 1]);
                    double z = Double.parseDouble(args[i + 2]);
                    loc = new Location(world, x, y, z);
                    i += 3;
                } catch (NumberFormatException e) {
                    sendMsg(sender, cfgMsg("coords-invalid"));
                    return;
                }
            }
        }

        // Check for -p flag
        if (i < args.length && args[i].equals("-p") && i + 1 < args.length) {
            viewPermission = args[i + 1];
        }

        // Validate amount
        int maxAmount = plugin.getConfigManager().getConfig().getInt("settings.max-amount", 50);
        if (amount < 1 || amount > maxAmount) {
            sendMsg(sender, cfgMsg("amount-invalid").replace("{max}", String.valueOf(maxAmount)));
            return;
        }

        // Check max mobs per player
        int maxPerPlayer = plugin.getConfigManager().getConfig().getInt("settings.max-mobs-per-player", 10);
        int currentCount = plugin.getSilentMobManager().getActiveCount(target.getUniqueId());
        if (currentCount + amount > maxPerPlayer) {
            sendMsg(sender, cfgMsg("max-mobs-reached")
                    .replace("{player}", target.getName())
                    .replace("{max}", String.valueOf(maxPerPlayer)));
            return;
        }

        // Spawn mobs
        int spawned = 0;
        for (int j = 0; j < amount; j++) {
            Entity entity = spawnMob(mobId, loc, level);
            if (entity != null) {
                SilentMob silentMob = new SilentMob(target, mobId, loc, level, false);
                silentMob.setEntity(entity);
                if (viewPermission != null) {
                    silentMob.setViewPermission(viewPermission);
                }
                plugin.getSilentMobManager().addSilentMob(silentMob);
                spawned++;
            }
        }

        if (spawned > 0) {
            String msg = cfgMsg("spawn-success")
                    .replace("{amount}", String.valueOf(spawned))
                    .replace("{mob}", mobId)
                    .replace("{player}", target.getName());
            if (viewPermission != null) {
                msg += " &7(perm: &b" + viewPermission + "&7)";
            }
            sendMsg(sender, msg);
        } else {
            sendMsg(sender, cfgMsg("spawn-fail").replace("{mob}", mobId));
        }
    }

    private Entity spawnMob(String mobId, Location loc, int level) {
        // Try MythicMobs first
        try {
            MythicBukkit mythic = MythicBukkit.inst();
            if (mythic != null && mythic.getMobManager().getMythicMob(mobId).isPresent()) {
                AbstractLocation mythicLoc = new AbstractLocation(
                        io.lumine.mythic.bukkit.BukkitAdapter.adapt(loc.getWorld()),
                        loc.getX(), loc.getY(), loc.getZ());
                ActiveMob activeMob = mythic.getMobManager().spawnMob(mobId, mythicLoc, level);
                return activeMob != null ? activeMob.getEntity().getBukkitEntity() : null;
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }

        // Fallback: vanilla EntityType
        try {
            EntityType type = EntityType.valueOf(mobId.toUpperCase());
            return loc.getWorld().spawnEntity(loc, type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==========================================
    // /silentmob despawn <player> [mob]
    // ==========================================
    private void handleDespawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMsg(sender, "&eUsage: &f/sm despawn <player> [mob]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMsg(sender, cfgMsg("player-offline").replace("{player}", args[1]));
            return;
        }

        int count;
        if (args.length >= 3) {
            count = plugin.getSilentMobManager().despawnByType(target.getUniqueId(), args[2]);
        } else {
            count = plugin.getSilentMobManager().despawnByPlayer(target.getUniqueId());
        }

        sendMsg(sender, cfgMsg("despawn-success")
                .replace("{player}", target.getName())
                .replace("{count}", String.valueOf(count)));
    }

    // ==========================================
    // /silentmob despawnall
    // ==========================================
    private void handleDespawnAll(CommandSender sender) {
        if (!sender.hasPermission("silentmob.admin")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return;
        }
        int count = plugin.getSilentMobManager().despawnAll();
        sendMsg(sender, cfgMsg("despawn-all-success").replace("{count}", String.valueOf(count)));
    }

    // ==========================================
    // /silentmob list [player]
    // ==========================================
    private void handleList(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMsg(sender, cfgMsg("player-offline").replace("{player}", args[1]));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sendMsg(sender, "&cUsage: /sm list <player>");
            return;
        }

        List<SilentMob> mobs = plugin.getSilentMobManager().getActiveMobs(target.getUniqueId());
        sendMsg(sender, cfgMsg("list-header").replace("{player}", target.getName()));

        if (mobs.isEmpty()) {
            sendMsg(sender, cfgMsg("list-empty"));
        } else {
            for (SilentMob mob : mobs) {
                Location l = mob.getEntity() != null ? mob.getEntity().getLocation() : mob.getSpawnLocation();
                String entry = cfgMsg("list-entry")
                        .replace("{mob}", mob.getMobId())
                        .replace("{world}", l.getWorld() != null ? l.getWorld().getName() : "?")
                        .replace("{x}", String.valueOf(l.getBlockX()))
                        .replace("{y}", String.valueOf(l.getBlockY()))
                        .replace("{z}", String.valueOf(l.getBlockZ()))
                        .replace("{age}", String.valueOf(mob.getAge()));
                if (mob.getViewPermission() != null) {
                    entry += " &b[perm: " + mob.getViewPermission() + "]";
                }
                if (mob.getRegionName() != null) {
                    entry += " &3[region: " + mob.getRegionName() + "]";
                }
                sendMsg(sender, entry);
            }
        }
    }

    // ==========================================
    // /silentmob global <on|off|status>
    // /silentmob global whitelist <add|remove> <mob>
    // ==========================================
    private void handleGlobal(CommandSender sender, String[] args) {
        if (!sender.hasPermission("silentmob.admin")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sendMsg(sender, "&eUsage: &f/sm global <on|off|status|whitelist>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "on" -> {
                plugin.getGlobalSilentManager().setEnabled(true);
                sendMsg(sender, cfgMsg("global-enabled"));
            }
            case "off" -> {
                plugin.getGlobalSilentManager().setEnabled(false);
                sendMsg(sender, cfgMsg("global-disabled"));
            }
            case "status" -> {
                String status = plugin.getGlobalSilentManager().isEnabled() ? "&2ON" : "&4OFF";
                int count = plugin.getSilentMobManager().getTotalCount();
                sendMsg(sender, cfgMsg("global-status")
                        .replace("{status}", status)
                        .replace("{count}", String.valueOf(count)));
            }
            case "whitelist" -> handleWhitelist(sender, args);
            default -> sendMsg(sender, "&eUsage: &f/sm global <on|off|status|whitelist>");
        }
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sendMsg(sender, "&eUsage: &f/sm global whitelist <add|remove> <mob>");
            return;
        }
        String action = args[2].toLowerCase();
        String mob = args[3];

        switch (action) {
            case "add" -> {
                if (plugin.getGlobalSilentConfig().addToWhitelist(mob)) {
                    plugin.getGlobalSilentConfig().save(plugin.getConfigManager().getConfig());
                    plugin.saveConfig();
                    sendMsg(sender, cfgMsg("whitelist-added").replace("{mob}", mob));
                } else {
                    sendMsg(sender, cfgMsg("whitelist-already").replace("{mob}", mob));
                }
            }
            case "remove" -> {
                if (plugin.getGlobalSilentConfig().removeFromWhitelist(mob)) {
                    plugin.getGlobalSilentConfig().save(plugin.getConfigManager().getConfig());
                    plugin.saveConfig();
                    sendMsg(sender, cfgMsg("whitelist-removed").replace("{mob}", mob));
                } else {
                    sendMsg(sender, cfgMsg("whitelist-not-found").replace("{mob}", mob));
                }
            }
            default -> sendMsg(sender, "&eUsage: &f/sm global whitelist <add|remove> <mob>");
        }
    }

    // ==========================================
    // /silentmob wand
    // ==========================================
    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, cfgMsg("region-only-player"));
            return;
        }
        if (!player.hasPermission("silentmob.admin")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return;
        }
        plugin.getWandManager().giveWand(player);
        sendMsg(sender, cfgMsg("wand-received"));
        sendMsg(sender, cfgMsg("wand-usage"));
    }

    // ==========================================
    // /silentmob region
    // <create|delete|list|info|addmob|removemob|exempt|unexempt|addplayer|removeplayer|addperm|removeperm>
    // ==========================================
    private void handleRegion(CommandSender sender, String[] args) {
        if (!sender.hasPermission("silentmob.admin")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sendMsg(sender,
                    "&eUsage: &f/sm region <create|delete|list|info|addmob|removemob|exempt|unexempt|addplayer|removeplayer|addperm|removeperm> ...");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> regionCreate(sender, args);
            case "delete" -> regionDelete(sender, args);
            case "list" -> regionList(sender);
            case "info" -> regionInfo(sender, args);
            case "addmob" -> regionModifyMob(sender, args, true, false);
            case "removemob" -> regionModifyMob(sender, args, false, false);
            case "exempt" -> regionModifyMob(sender, args, true, true);
            case "unexempt" -> regionModifyMob(sender, args, false, true);
            case "addplayer" -> regionModifyPlayer(sender, args, true);
            case "removeplayer" -> regionModifyPlayer(sender, args, false);
            case "addperm" -> regionModifyPerm(sender, args, true);
            case "removeperm" -> regionModifyPerm(sender, args, false);
            default -> sendMsg(sender, "&eUnknown region subcommand: &f" + args[1]);
        }
    }

    private void regionCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMsg(sender, cfgMsg("region-only-player"));
            return;
        }
        if (args.length < 3) {
            sendMsg(sender, "&eUsage: &f/sm region create <name>");
            return;
        }
        if (!plugin.getWandManager().hasBothPositions(player)) {
            sendMsg(sender, cfgMsg("region-no-wand"));
            return;
        }
        String name = args[2].toLowerCase();
        Location pos1 = plugin.getWandManager().getPos(player, 1);
        Location pos2 = plugin.getWandManager().getPos(player, 2);

        SilentRegion region = plugin.getRegionManager().createRegion(name, pos1, pos2);
        if (region == null) {
            sendMsg(sender, cfgMsg("region-exists").replace("{name}", name));
            return;
        }
        plugin.getWandManager().clearSelection(player);
        sendMsg(sender, cfgMsg("region-created").replace("{name}", name) + " (" +
                pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ() + " → " +
                pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ() + ")");
    }

    private void regionDelete(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&eUsage: &f/sm region delete <name>");
            return;
        }
        if (plugin.getRegionManager().deleteRegion(args[2])) {
            sendMsg(sender, cfgMsg("region-deleted").replace("{name}", args[2]));
        } else {
            sendMsg(sender, cfgMsg("region-not-found").replace("{name}", args[2]));
        }
    }

    private void regionList(CommandSender sender) {
        var regions = plugin.getRegionManager().getAllRegions();
        if (regions.isEmpty()) {
            sendMsg(sender, "&7No regions defined");
            return;
        }
        sendMsg(sender, "&b&lSilent Regions &7(" + regions.size() + "):");
        for (SilentRegion r : regions) {
            sendMsg(sender, "&7- &b" + r.getName() + " &7[" + r.getWorldName() + "] " +
                    r.getX1() + "," + r.getY1() + "," + r.getZ1() + " → " +
                    r.getX2() + "," + r.getY2() + "," + r.getZ2() +
                    " &7mobs:" + r.getSilentMobs().size() +
                    " players:" + r.getAllowedPlayers().size() +
                    " perms:" + r.getAllowedPermissions().size());
        }
    }

    private void regionInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMsg(sender, "&eUsage: &f/sm region info <name>");
            return;
        }
        SilentRegion r = plugin.getRegionManager().getRegion(args[2]);
        if (r == null) {
            sendMsg(sender, cfgMsg("region-not-found").replace("{name}", args[2]));
            return;
        }
        sendMsg(sender, "&b&l" + r.getName() + " &7— Region Info");
        sendMsg(sender, "&7World: &f" + r.getWorldName());
        sendMsg(sender, "&7Pos1: &f" + r.getX1() + ", " + r.getY1() + ", " + r.getZ1());
        sendMsg(sender, "&7Pos2: &f" + r.getX2() + ", " + r.getY2() + ", " + r.getZ2());
        sendMsg(sender,
                "&7Silent mobs: &f" + (r.getSilentMobs().isEmpty() ? "ALL" : String.join(", ", r.getSilentMobs())));
        sendMsg(sender,
                "&7Exempt mobs: &f" + (r.getExemptMobs().isEmpty() ? "none" : String.join(", ", r.getExemptMobs())));

        Set<UUID> players = r.getAllowedPlayers();
        if (players.isEmpty()) {
            sendMsg(sender, "&7Allowed players: &fnone");
        } else {
            StringBuilder sb = new StringBuilder();
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                sb.append(p != null ? p.getName() : uuid.toString()).append(", ");
            }
            sendMsg(sender, "&7Allowed players: &f" + sb.substring(0, sb.length() - 2));
        }
        sendMsg(sender, "&7Allowed permissions: &f"
                + (r.getAllowedPermissions().isEmpty() ? "none" : String.join(", ", r.getAllowedPermissions())));
    }

    private void regionModifyMob(CommandSender sender, String[] args, boolean add, boolean exempt) {
        if (args.length < 4) {
            sendMsg(sender, "&eUsage: &f/sm region " + args[1] + " <region> <mob>");
            return;
        }
        SilentRegion r = plugin.getRegionManager().getRegion(args[2]);
        if (r == null) {
            sendMsg(sender, cfgMsg("region-not-found").replace("{name}", args[2]));
            return;
        }
        String mob = args[3].toUpperCase();
        if (exempt) {
            if (add)
                r.addExemptMob(mob);
            else
                r.removeExemptMob(mob);
            sendMsg(sender, (add ? "&aAdded" : "&cRemoved") + " &e" + mob + (add ? " &ato" : " &cfrom")
                    + " exempt list of &b" + r.getName());
        } else {
            if (add)
                r.addSilentMob(mob);
            else
                r.removeSilentMob(mob);
            sendMsg(sender, (add ? "&aAdded" : "&cRemoved") + " &e" + mob + (add ? " &ato" : " &cfrom")
                    + " silent list of &b" + r.getName());
        }
        plugin.getRegionManager().saveRegions();
    }

    private void regionModifyPlayer(CommandSender sender, String[] args, boolean add) {
        if (args.length < 4) {
            sendMsg(sender, "&eUsage: &f/sm region " + args[1] + " <region> <player>");
            return;
        }
        SilentRegion r = plugin.getRegionManager().getRegion(args[2]);
        if (r == null) {
            sendMsg(sender, cfgMsg("region-not-found").replace("{name}", args[2]));
            return;
        }
        Player target = Bukkit.getPlayer(args[3]);
        UUID uuid;
        String name;
        if (target != null) {
            uuid = target.getUniqueId();
            name = target.getName();
        } else {
            try {
                uuid = UUID.fromString(args[3]);
                name = args[3];
            } catch (IllegalArgumentException e) {
                sendMsg(sender, "&cPlayer &e" + args[3] + " &cnot found and not a valid UUID");
                return;
            }
        }
        if (add)
            r.addAllowedPlayer(uuid);
        else
            r.removeAllowedPlayer(uuid);
        sendMsg(sender, (add ? "&aAdded" : "&cRemoved") + " &e" + name + (add ? " &ato" : " &cfrom")
                + " allowed players of &b" + r.getName());
        plugin.getRegionManager().saveRegions();
    }

    private void regionModifyPerm(CommandSender sender, String[] args, boolean add) {
        if (args.length < 4) {
            sendMsg(sender, "&eUsage: &f/sm region " + args[1] + " <region> <permission>");
            return;
        }
        SilentRegion r = plugin.getRegionManager().getRegion(args[2]);
        if (r == null) {
            sendMsg(sender, cfgMsg("region-not-found").replace("{name}", args[2]));
            return;
        }
        String perm = args[3];
        if (add)
            r.addAllowedPermission(perm);
        else
            r.removeAllowedPermission(perm);
        sendMsg(sender, (add ? "&aAdded" : "&cRemoved") + " &e" + perm + (add ? " &ato" : " &cfrom")
                + " allowed permissions of &b" + r.getName());
        plugin.getRegionManager().saveRegions();
    }

    // ==========================================
    // /silentmob reload
    // ==========================================
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("silentmob.admin")) {
            sendMsg(sender, cfgMsg("no-permission"));
            return;
        }
        plugin.getConfigManager().reloadAll();
        plugin.getGlobalSilentConfig().load(plugin.getConfigManager().getConfig());
        plugin.getRegionManager().loadRegions();
        plugin.getPartyHook().refresh();
        sendMsg(sender, cfgMsg("reload-success"));
    }

    // ==========================================
    // Utility
    // ==========================================
    private String cfgMsg(String key) {
        return plugin.getConfigManager().getMessage(key);
    }

    private void sendMsg(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
