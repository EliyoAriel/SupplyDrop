package com.supplydrop.helpers;

import com.supplydrop.Crate;
import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class RewindIntegration {

    private static boolean enabled = false;
    private static int radius = 1;
    private static Object api;
    private static Method excludeArea;
    private static Method unexcludeArea;

    private RewindIntegration() {}

    public static void init() {
        try {
            Plugin rewind = Bukkit.getPluginManager().getPlugin("Rewind");
            if (rewind == null || !rewind.isEnabled()) {
                enabled = false;
                return;
            }

            Class<?> pluginClass = Class.forName("com.rewind.RewindPlugin");
            Method getAPI = pluginClass.getMethod("getAPI");
            api = getAPI.invoke(null);
            if (api == null) {
                enabled = false;
                return;
            }

            excludeArea = api.getClass().getMethod("excludeChunkArea", String.class, int.class, int.class, int.class, int.class);
            unexcludeArea = api.getClass().getMethod("unexcludeChunkArea", String.class, int.class, int.class, int.class, int.class);

            enabled = SupplyDrop.getConfiguration().getConfig().getBoolean("rewind-integration.enabled", true);
            radius = SupplyDrop.getConfiguration().getConfig().getInt("rewind-integration.exclusion-radius", 1);

            AirdropLogger.debug("Rewind integration enabled (radius=" + radius + ")");
        } catch (Exception e) {
            enabled = false;
            api = null;
            excludeArea = null;
            unexcludeArea = null;
        }
    }

    public static void onCrateLand(Crate crate) {
        if (!enabled || api == null) return;
        Location loc = crate.getLandedLocation();
        if (loc == null || loc.getWorld() == null) return;
        excludeArea(loc.getWorld().getName(), loc);
    }

    public static void onCrateDestroy(Crate crate) {
        if (!enabled || api == null) return;
        Location loc = crate.getLandedLocation();
        if (loc == null || loc.getWorld() == null) return;
        unexcludeArea(loc.getWorld().getName(), loc);
    }

    public static void registerActiveCrates() {
        if (!enabled || api == null) return;
        for (Crate crate : CrateManager.getActiveCrates()) {
            Location loc = crate.getLandedLocation();
            if (loc == null || loc.getWorld() == null) continue;
            excludeArea(loc.getWorld().getName(), loc);
        }
        int count = CrateManager.getActiveCrates().size();
        if (count > 0) {
            AirdropLogger.debug("Restored Rewind exclusion for " + count + " active crate(s)");
        }
    }

    private static void excludeArea(String world, Location loc) {
        try {
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            excludeArea.invoke(api, world, cx - radius, cz - radius, cx + radius, cz + radius);
        } catch (Exception ignored) {}
    }

    private static void unexcludeArea(String world, Location loc) {
        try {
            int cx = loc.getBlockX() >> 4;
            int cz = loc.getBlockZ() >> 4;
            unexcludeArea.invoke(api, world, cx - radius, cz - radius, cx + radius, cz + radius);
        } catch (Exception ignored) {}
    }
}
