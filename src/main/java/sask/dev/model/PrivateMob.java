package sask.dev.model;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.api.adapters.AbstractLocation;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PrivateMob {

    private final Plugin plugin;
    private final Player viewer;
    private final String mobId;
    private final Location location;

    private Entity entity;
    private ActiveMob mythicMob;

    public PrivateMob(Plugin plugin, Player viewer, String mobId, Location location) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.mobId = mobId;
        this.location = location;
    }

    public void spawn() {

        // ——————————————
        // ¿Es MythicMobs?
        // ——————————————
        MythicBukkit mythic = MythicBukkit.inst();
        var opt = mythic.getMobManager().getMythicMob(mobId);

        if (opt.isPresent()) {
            var mythicTemplate = opt.get();
            AbstractLocation abs = new AbstractLocation(
                    location.getWorld().getName(),
                    location.getX(),
                    location.getY(),
                    location.getZ()
            );

            mythicMob = mythicTemplate.spawn(abs, 1);
            if (mythicMob != null) {
                entity = mythicMob.getEntity().getBukkitEntity();
            }
        } else {

            // ——————————————
            // Mob VANILLA
            // ——————————————
            EntityType type;
            try {
                type = EntityType.valueOf(mobId.toUpperCase());
            } catch (Exception e) {
                viewer.sendMessage("§cMob no encontrado: §e" + mobId);
                return;
            }

            entity = location.getWorld().spawnEntity(location, type);
        }

        if (entity == null) {
            viewer.sendMessage("§cNo se pudo spawnear el mob.");
            return;
        }

        hideFromOthers();
    }

    private void hideFromOthers() {
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(viewer)) {
                online.hideEntity(plugin, entity);
            }
        }
    }

    public void despawn() {
        if (entity != null && entity.isValid()) {
            entity.remove();
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public Player getViewer() {
        return viewer;
    }
}
