package com.supplydrop.helpers;

import com.supplydrop.Crate;
import com.supplydrop.Crate.State;
import com.supplydrop.config.ConfigKeys;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders merged particle borders for LOCK/READY crates.
 * When multiple crates overlap, only the outermost border is shown.
 * Block protection is handled by AntiGriefListener.
 */
public class ZoneManager {

    private static BukkitTask task;

    private ZoneManager() {}

    public static void start(JavaPlugin plugin) {
        if (task != null) return;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public static void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static void tick() {
        List<Zone> zones = collectZones();
        if (zones.isEmpty()) return;

        World world = zones.get(0).world();
        double radius = ConfigKeys.getZoneRadius();
        renderMergedBorder(world, zones, radius);
    }

    private static List<Zone> collectZones() {
        List<Zone> zones = new ArrayList<>();
        for (Crate crate : CrateManager.getActiveCrates()) {
            State state = crate.getState();
            if (state != State.LOCK && state != State.READY_TO_OPEN) continue;
            Location loc = crate.getLocation();
            if (loc == null || loc.getWorld() == null) continue;
            zones.add(new Zone(loc.getWorld(), loc.getX(), loc.getZ()));
        }
        return zones;
    }

    /**
     * Check if a location is inside any LOCK/READY crate's zone.
     */
    public static boolean isInZone(Location loc) {
        if (!ConfigKeys.isZoneEnabled()) return false;
        double radius = ConfigKeys.getZoneRadius();
        for (Crate crate : CrateManager.getActiveCrates()) {
            State state = crate.getState();
            if (state != State.LOCK && state != State.READY_TO_OPEN) continue;
            Location crateLoc = crate.getLocation();
            if (crateLoc == null || crateLoc.getWorld() == null) continue;
            if (!crateLoc.getWorld().equals(loc.getWorld())) continue;
            double dx = loc.getX() - crateLoc.getX();
            double dz = loc.getZ() - crateLoc.getZ();
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    /**
     * Render the merged border: for each zone, sample points around its circle.
     * Keep only points that are NOT inside any other zone — this produces the union outline.
     */
    private static void renderMergedBorder(World world, List<Zone> zones, double radius) {
        Particle particle = parseParticle(ConfigKeys.getZoneParticle());
        double y = zones.get(0).world().getHighestBlockYAt(
                (int) zones.get(0).x(), (int) zones.get(0).z()) + 1.0;

        int pointsPerZone = Math.max(40, (int) (radius * 3));

        for (Zone zone : zones) {
            double angleStep = Math.PI * 2.0 / pointsPerZone;

            for (int i = 0; i < pointsPerZone; i++) {
                double angle = i * angleStep;
                double px = zone.x() + radius * Math.cos(angle);
                double pz = zone.z() + radius * Math.sin(angle);

                boolean insideOther = false;
                for (Zone other : zones) {
                    if (other == zone) continue;
                    double dx = px - other.x();
                    double dz = pz - other.z();
                    if (dx * dx + dz * dz < radius * radius) {
                        insideOther = true;
                        break;
                    }
                }

                if (!insideOther) {
                    Location point = new Location(world, px, y, pz);
                    world.spawnParticle(particle, point, 1, 0, 0, 0, 0.01);
                }
            }
        }
    }

    private static Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }

    private record Zone(World world, double x, double z) {}
}
