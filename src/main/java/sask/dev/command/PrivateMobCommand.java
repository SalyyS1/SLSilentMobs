package sask.dev.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sask.dev.PrivateMobs;
import sask.dev.model.PrivateMob;

public class PrivateMobCommand implements CommandExecutor {

    private final PrivateMobs plugin;

    public PrivateMobCommand(PrivateMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 2) {
            sender.sendMessage("§eUso: §f/privatemob spawn <player> <mob> [world] [x] [y] [z]");
            return true;
        }

        Player viewer = Bukkit.getPlayer(args[0]);
        if (viewer == null) {
            sender.sendMessage("§cJugador offline");
            return true;
        }

        String mobId = args[1];
        Location loc;

        // Ubicación opcional
        if (args.length >= 6) {
            World world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage("§cMundo inválido");
                return true;
            }
            double x = Double.parseDouble(args[3]);
            double y = Double.parseDouble(args[4]);
            double z = Double.parseDouble(args[5]);

            loc = new Location(world, x, y, z);
        } else {
            loc = viewer.getLocation();
        }

        PrivateMob pm = new PrivateMob(plugin, viewer, mobId, loc);
        pm.spawn();

        plugin.getPrivateMobManager().addPrivateMob(viewer, pm);

        sender.sendMessage("§aMob privado spawneado para §e" + viewer.getName());
        return true;
    }
}
