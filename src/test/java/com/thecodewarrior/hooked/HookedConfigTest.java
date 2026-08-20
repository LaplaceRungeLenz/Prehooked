package com.thecodewarrior.hooked;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraftforge.common.config.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.thecodewarrior.hooked.common.HookSettings;
import com.thecodewarrior.hooked.common.HookStats;
import com.thecodewarrior.hooked.common.HookType;

public class HookedConfigTest {

    private static final double EPSILON = 1.0E-9D;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Field minecraftHome;
    private Object previousMinecraftHome;

    @Before
    public void initializeForgeConfigurationEnvironment() throws Exception {
        minecraftHome = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousMinecraftHome = minecraftHome.get(null);
        minecraftHome.set(null, temporaryFolder.getRoot());
    }

    @After
    public void restoreDefaults() throws Exception {
        HookedConfig.load(new File(temporaryFolder.getRoot(), "defaults.cfg"));
        minecraftHome.set(null, previousMinecraftHome);
    }

    @Test
    public void loadsEveryConfigurableHookStatistic() throws Exception {
        File file = temporaryFolder.newFile("hooked.cfg");
        Configuration config = new Configuration(file);
        config.load();
        config.get(Configuration.CATEGORY_GENERAL, "searchLocations", 1)
            .set(15);
        config.get(Configuration.CATEGORY_GENERAL, "enableRedHookFlight", true)
            .set(false);
        config.get("hooks.iron", "maxAnchors", 2)
            .set(7);
        config.get("hooks.iron", "rangeBlocks", 16.0D)
            .set(72.0D);
        config.get("hooks.iron", "projectileSpeedBlocksPerTick", 3.6D)
            .set(8.0D);
        config.get("hooks.iron", "pullSpeedBlocksPerTick", 0.4D)
            .set(3.0D);
        config.get("hooks.iron", "retractSpeedBlocksPerTick", 0.5D)
            .set(4.0D);
        config.save();

        HookedConfig.load(file);

        HookSettings settings = HookedConfig.getLocalSettings();
        HookStats iron = settings.getStats(HookType.IRON);
        assertEquals(15, settings.getSearchLocations());
        assertTrue(!settings.isRedHookFlightEnabled());
        assertEquals(7, iron.getMaxAnchors());
        assertEquals(72.0D, iron.getRange(), EPSILON);
        assertEquals(8.0D, iron.getProjectileSpeed(), EPSILON);
        assertEquals(3.0D, iron.getPullSpeed(), EPSILON);
        assertEquals(4.0D, iron.getRetractSpeed(), EPSILON);
    }

    @Test
    public void generatesAllHookCategoriesWithCurrentDefaults() {
        File file = new File(temporaryFolder.getRoot(), "generated.cfg");

        HookedConfig.load(file);

        Configuration generated = new Configuration(file);
        generated.load();
        for (HookType type : HookType.values()) {
            String category = "hooks." + type.getSerializedName();
            assertTrue(generated.hasCategory(category));
            assertTrue(
                generated.getCategory(category)
                    .containsKey("maxAnchors"));
            assertTrue(
                generated.getCategory(category)
                    .containsKey("rangeBlocks"));
            assertTrue(
                generated.getCategory(category)
                    .containsKey("projectileSpeedBlocksPerTick"));
            assertTrue(
                generated.getCategory(category)
                    .containsKey("pullSpeedBlocksPerTick"));
            assertTrue(
                generated.getCategory(category)
                    .containsKey("retractSpeedBlocksPerTick"));
        }
    }
}
