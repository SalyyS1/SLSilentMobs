package sask.dev.command;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import sask.dev.PrivateMobs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrivateMobTab implements TabCompleter {

    private final PrivateMobs plugin;

    public PrivateMobTab(PrivateMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> list = new ArrayList<>();

        // ────────────────────────────────
        // /privatemob <player> <mob> ...
        // ────────────────────────────────

        // ARG 0 → Jugadores online
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // ARG 1 → Mobs Vanilla + Mobs de MythicMobs
        if (args.length == 2) {
            // Vanilla EntityTypes
            for (EntityType type : EntityType.values()) {
                if (type.isAlive()) {
                    list.add(type.name());
                }
            }

            // MythicMobs
            MythicBukkit m = MythicBukkit.inst();
            if (m != null) {
                m.getMobManager().getMobNames().forEach(list::add);
            }

            // Filtrar según lo escrito
            return list.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        // ARG 2 → Mundos
        if (args.length == 3) {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(w -> w.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // ARG 3/4/5 → Coordenadas
        if (args.length == 4 || args.length == 5 || args.length == 6) {
            if (sender instanceof Player player) {
                double coord = switch (args.length) {
                    case 4 -> player.getLocation().getX();
                    case 5 -> player.getLocation().getY();
                    case 6 -> player.getLocation().getZ();
                    default -> 0;
                };

                return List.of(String.valueOf((int) coord));
            }
        }

        return List.of();
    }
}
