package vn.saly.silentmobs.region;

/**
 * Mob spawn rule triggered when a player enters a silent region.
 */
public class RegionSpawnEntry {

    private final String mobId;
    private final int amount;
    private final int level;
    private final int cooldownSeconds;
    private final double spread;

    public RegionSpawnEntry(String mobId, int amount, int level, int cooldownSeconds, double spread) {
        this.mobId = mobId;
        this.amount = Math.max(1, amount);
        this.level = Math.max(1, level);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.spread = Math.max(0, spread);
    }

    public String getMobId() {
        return mobId;
    }

    public int getAmount() {
        return amount;
    }

    public int getLevel() {
        return level;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public double getSpread() {
        return spread;
    }
}
