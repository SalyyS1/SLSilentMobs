package vn.saly.silentmobs.region;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import vn.saly.silentmobs.SLSilentMobs;

/**
 * Handles wand click events for region position selection.
 * Left click = pos1, Right click = pos2.
 */
public class WandListener implements Listener {

    private final SLSilentMobs plugin;

    public WandListener(SLSilentMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getWandManager().isWand(event.getItem()))
            return;
        if (!player.hasPermission("silentmob.admin"))
            return;

        Action action = event.getAction();
        Block block = event.getClickedBlock();

        if (action == Action.LEFT_CLICK_BLOCK && block != null) {
            event.setCancelled(true);
            Location loc = block.getLocation();
            plugin.getWandManager().setPos(player, 1, loc);
            player.sendMessage(ChatColor.AQUA + "[SilentMobs] " + ChatColor.GREEN + "Pos1 " +
                    ChatColor.GRAY + "set to " + ChatColor.WHITE +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() +
                    ChatColor.GRAY + " (" + loc.getWorld().getName() + ")");

        } else if (action == Action.RIGHT_CLICK_BLOCK && block != null) {
            event.setCancelled(true);
            Location loc = block.getLocation();
            plugin.getWandManager().setPos(player, 2, loc);
            player.sendMessage(ChatColor.AQUA + "[SilentMobs] " + ChatColor.GREEN + "Pos2 " +
                    ChatColor.GRAY + "set to " + ChatColor.WHITE +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() +
                    ChatColor.GRAY + " (" + loc.getWorld().getName() + ")");

            // If both positions set, show info
            if (plugin.getWandManager().hasBothPositions(player)) {
                Location p1 = plugin.getWandManager().getPos(player, 1);
                Location p2 = plugin.getWandManager().getPos(player, 2);
                int sizeX = Math.abs(p2.getBlockX() - p1.getBlockX()) + 1;
                int sizeY = Math.abs(p2.getBlockY() - p1.getBlockY()) + 1;
                int sizeZ = Math.abs(p2.getBlockZ() - p1.getBlockZ()) + 1;
                player.sendMessage(ChatColor.AQUA + "[SilentMobs] " + ChatColor.GRAY +
                        "Selection: " + ChatColor.WHITE + sizeX + "x" + sizeY + "x" + sizeZ +
                        ChatColor.GRAY + " | Use " + ChatColor.AQUA + "/sm region create <name>");
            }
        }
    }
}
