package vn.saly.silentmobs.hook;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.party.AbstractParty;
import org.bukkit.entity.Player;
import vn.saly.silentmobs.SLSilentMobs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Defensive adapter around the MMOCore party API.
 *
 * All MMOCore class references are isolated in this class so the rest of the
 * plugin never touches MMOCore types directly. When MMOCore is absent, the
 * static reference resolution throws {@link NoClassDefFoundError} on first use;
 * every method swallows that and degrades to "no party" behaviour.
 *
 * Activation requires BOTH the config flag (party.share-with-party) AND the
 * MMOCore plugin being present and enabled.
 */
public class PartyHook {

    private final SLSilentMobs plugin;
    private boolean active;

    public PartyHook(SLSilentMobs plugin) {
        this.plugin = plugin;
        refresh();
    }

    /**
     * Recompute activation from config + plugin presence.
     * Called on enable and on /sm reload.
     */
    public void refresh() {
        boolean configEnabled = plugin.getConfigManager().getConfig()
                .getBoolean("party.share-with-party", false);
        boolean pluginPresent = plugin.getServer().getPluginManager().isPluginEnabled("MMOCore");
        this.active = configEnabled && pluginPresent;
    }

    /**
     * @return true if party sharing should be applied.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Check if two players are in the same MMOCore party.
     * Owner-self is intentionally NOT treated as a party relation here — the
     * caller already handles owner visibility separately.
     *
     * @return true only when both are loaded, in a party, and that party
     *         contains the other player.
     */
    public boolean isInSameParty(UUID ownerUUID, Player other) {
        if (!active || ownerUUID == null || other == null)
            return false;
        if (ownerUUID.equals(other.getUniqueId()))
            return false;
        try {
            if (!PlayerData.has(ownerUUID))
                return false;
            PlayerData ownerData = PlayerData.get(ownerUUID);
            AbstractParty party = MMOCore.plugin.partyModule.getParty(ownerData);
            if (party == null)
                return false;
            return party.hasMember(other);
        } catch (NoClassDefFoundError | Exception ignored) {
            return false;
        }
    }

    /**
     * Get the online party members of the given owner, EXCLUDING the owner.
     * Returns an empty list when party sharing is inactive or the owner has no
     * party.
     */
    public List<Player> getOnlinePartyMembers(UUID ownerUUID) {
        List<Player> result = new ArrayList<>();
        if (!active || ownerUUID == null)
            return result;
        try {
            if (!PlayerData.has(ownerUUID))
                return result;
            PlayerData ownerData = PlayerData.get(ownerUUID);
            AbstractParty party = MMOCore.plugin.partyModule.getParty(ownerData);
            if (party == null)
                return result;

            for (PlayerData member : party.getOnlineMembers()) {
                Player p = member.getPlayer();
                if (p != null && !p.getUniqueId().equals(ownerUUID)) {
                    result.add(p);
                }
            }
        } catch (NoClassDefFoundError | Exception ignored) {
        }
        return result;
    }
}
