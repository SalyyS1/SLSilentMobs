package vn.saly.silentmobs.region;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * Manages the selection wand tool and player pos1/pos2 selections.
 */
public class WandManager {

    private final NamespacedKey wandKey;

    // Player UUID -> [pos1, pos2]
    private final Map<UUID, Location[]> selections = new HashMap<>();

    public WandManager(Plugin plugin) {
        this.wandKey = new NamespacedKey(plugin, "slsilent_wand");
    }

    /**
     * Give a wand tool to a player.
     */
    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(Material.STICK);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "SilentMobs Wand");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Left Click " + ChatColor.WHITE + "→ Set Pos1",
                    ChatColor.GRAY + "Right Click " + ChatColor.WHITE + "→ Set Pos2",
                    "",
                    ChatColor.DARK_GRAY + "Use /silentmob region create <name>"));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
            wand.setItemMeta(meta);
        }
        player.getInventory().addItem(wand);
    }

    /**
     * Check if an ItemStack is a SilentMobs wand.
     */
    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.STICK)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    /**
     * Set pos1 or pos2 for a player.
     * 
     * @param posNumber 1 or 2
     */
    public void setPos(Player player, int posNumber, Location loc) {
        UUID uuid = player.getUniqueId();
        Location[] locs = selections.computeIfAbsent(uuid, k -> new Location[2]);
        locs[posNumber - 1] = loc.clone();
    }

    /**
     * Get pos1 or pos2 for a player.
     * 
     * @param posNumber 1 or 2
     */
    public Location getPos(Player player, int posNumber) {
        Location[] locs = selections.get(player.getUniqueId());
        if (locs == null)
            return null;
        return locs[posNumber - 1];
    }

    /**
     * Check if both positions are set.
     */
    public boolean hasBothPositions(Player player) {
        return getPos(player, 1) != null && getPos(player, 2) != null;
    }

    /**
     * Clear selections for a player.
     */
    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }

    public NamespacedKey getWandKey() {
        return wandKey;
    }
}
