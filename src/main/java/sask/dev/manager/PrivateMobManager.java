package sask.dev.manager;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import sask.dev.model.PrivateMob;

import java.util.*;

public class PrivateMobManager {

    private final Plugin plugin;

    // Lista de mobs privados por jugador
    private final Map<UUID, List<PrivateMob>> privateMobs = new HashMap<>();

    public PrivateMobManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void addPrivateMob(Player viewer, PrivateMob mob) {
        privateMobs.computeIfAbsent(viewer.getUniqueId(), k -> new ArrayList<>()).add(mob);
    }

    public void despawnAll() {
        for (List<PrivateMob> list : privateMobs.values()) {
            for (PrivateMob mob : list) {
                mob.despawn();
            }
        }
        privateMobs.clear();
    }

    public void hideForOthers(Player viewer, Entity entity) {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(viewer)) {
                online.hideEntity(plugin, entity);
            }
        }
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
