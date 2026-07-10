package vn.saly.silentmobs.region;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentRegionTest {

    @Test
    void mobNormalizationIsLocaleIndependent() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            SilentRegion region = region();
            region.addSilentMob("pillager");
            assertTrue(region.isMobSilent("PILLAGER"));

            region.addExemptMob("illusioner");
            assertFalse(region.isMobSilent("ILLUSIONER"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void addingSameSpawnMobReplacesExistingRule() {
        SilentRegion region = region();
        region.addSpawnEntry(new RegionSpawnEntry("RoadWolf", 1, 1, 60, 4));
        region.addSpawnEntry(new RegionSpawnEntry("roadwolf", 3, 5, 30, 8));

        assertEquals(1, region.getSpawnEntries().size());
        RegionSpawnEntry entry = region.getSpawnEntries().get(0);
        assertEquals(3, entry.getAmount());
        assertEquals(5, entry.getLevel());
        assertEquals(30, entry.getCooldownSeconds());
        assertEquals(8, entry.getSpread());
    }

    private SilentRegion region() {
        return new SilentRegion("road", "world", 0, 0, 0, 10, 10, 10);
    }
}
