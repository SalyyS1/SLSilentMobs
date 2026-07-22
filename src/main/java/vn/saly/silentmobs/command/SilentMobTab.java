package vn.saly.silentmobs.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;
import vn.saly.silentmobs.region.SilentRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tab completion for /silentmob commands.
 */
public class SilentMobTab implements TabCompleter {

    private final SLSilentMobs plugin;

    private static final List<String> ROOT_CMDS = Arrays.asList(
            "spawn", "despawn", "despawnall", "list", "global", "reload", "debug", "wand", "region", "help");
    private static final List<String> GLOBAL_SUBS = Arrays.asList("on", "off", "status", "whitelist");
    private static final List<String> WHITELIST_ACTIONS = Arrays.asList("add", "remove");
    private static final List<String> REGION_SUBS = Arrays.asList(
            "create", "delete", "list", "info",
            "addmob", "removemob", "exempt", "unexempt",
            "addplayer", "removeplayer", "addperm", "removeperm",
            "addspawn", "removespawn", "listspawns");

    public SilentMobTab(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("silentmob.use"))
            return Collections.emptyList();

        if (args.length == 1) {
            return filter(ROOT_CMDS, args[0]);
        }

        switch (args[0].toLowerCase()) {
            case "spawn":
                return handleSpawnTab(args);
            case "despawn":
                return handleDespawnTab(args);
            case "list":
                if (args.length == 2)
                    return filterPlayers(args[1]);
                break;
            case "global":
                return handleGlobalTab(args);
            case "region":
                return handleRegionTab(sender, args);
        }

        return Collections.emptyList();
    }

    private List<String> handleSpawnTab(String[] args) {
        switch (args.length) {
            case 2:
                return filterPlayers(args[1]);
            case 3:
                return filterMobTypes(args[2]);
            case 4:
                return Arrays.asList("1", "5", "10");
            case 5:
                return Arrays.asList("1", "5", "10");
            default:
                // Check for -p flag
                if (args.length >= 4) {
                    String prev = args[args.length - 2];
                    if (prev.equals("-p"))
                        return Arrays.asList("vip.premium", "silentmob.view");
                    return filter(Arrays.asList("-p"), args[args.length - 1]);
                }
        }
        return Collections.emptyList();
    }

    private List<String> handleDespawnTab(String[] args) {
        if (args.length == 2)
            return filterPlayers(args[1]);
        if (args.length == 3)
            return filterMobTypes(args[2]);
        return Collections.emptyList();
    }

    private List<String> handleGlobalTab(String[] args) {
        if (args.length == 2)
            return filter(GLOBAL_SUBS, args[1]);
        if (args.length == 3 && args[1].equalsIgnoreCase("whitelist"))
            return filter(WHITELIST_ACTIONS, args[2]);
        if (args.length == 4 && args[1].equalsIgnoreCase("whitelist"))
            return filterMobTypes(args[3]);
        return Collections.emptyList();
    }

    private List<String> handleRegionTab(CommandSender sender, String[] args) {
        if (args.length == 2)
            return filter(REGION_SUBS, args[1]);

        String sub = args[1].toLowerCase();
        if (args.length == 3) {
            switch (sub) {
                case "create":
                    return Collections.emptyList(); // User types name
                case "list":
                    return Collections.emptyList();
                default:
                    return filterRegionNames(args[2]); // All others need region name
            }
        }

        if (args.length == 4) {
            switch (sub) {
                case "addmob", "removemob", "exempt", "unexempt", "addspawn":
                    return filterMobTypes(args[3]);
                case "removespawn":
                    return filterRegionSpawnNames(args[2], args[3]);
                case "addplayer", "removeplayer":
                    return filterPlayers(args[3]);
                case "addperm", "removeperm":
                    return Arrays.asList("silentmob.region.", "vip.", "rank.");
            }
        }
        if (args.length == 5 && sub.equals("addspawn"))
            return Arrays.asList("1", "2", "3", "5");
        if (args.length == 6 && sub.equals("addspawn"))
            return Arrays.asList("1", "5", "10");
        if (args.length == 7 && sub.equals("addspawn"))
            return Arrays.asList("30", "60", "120", "300");
        if (args.length == 8 && sub.equals("addspawn"))
            return Arrays.asList("0", "4", "8", "12");

        return Collections.emptyList();
    }

    // --- Utility ---

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> filterPlayers(String input) {
        String lower = input.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> filterMobTypes(String input) {
        String upper = input.toUpperCase();
        List<String> types = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type.isAlive() && type.name().startsWith(upper)) {
                types.add(type.name());
            }
        }

        // Also try MythicMobs
        try {
            var mythic = io.lumine.mythic.bukkit.MythicBukkit.inst();
            if (mythic != null) {
                mythic.getMobManager().getMobTypes().forEach(mm -> {
                    if (mm.getInternalName().toUpperCase().startsWith(upper)) {
                        types.add(mm.getInternalName());
                    }
                });
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }

        return types;
    }

    private List<String> filterRegionNames(String input) {
        String lower = input.toLowerCase();
        return plugin.getRegionManager().getAllRegions().stream()
                .map(SilentRegion::getName)
                .filter(name -> name.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> filterRegionSpawnNames(String regionName, String input) {
        SilentRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null)
            return Collections.emptyList();

        String lower = input.toLowerCase();
        return region.getSpawnEntries().stream()
                .map(entry -> entry.getMobId())
                .filter(name -> name.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
