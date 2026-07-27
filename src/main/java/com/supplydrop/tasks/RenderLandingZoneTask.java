package com.supplydrop.tasks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.supplydrop.config.ConfigKeys;

/**
 * Renders a particle ring on the ground showing where the crate will land.
 */
public class RenderLandingZoneTask extends BukkitRunnable {

    private final World world;
    private final Location center;
    private final int radius;
    private final Particle particle;
    private int tick = 0;

    public RenderLandingZoneTask(Location center, World world) {
        this.world = world;
        this.center = center.clone();
        this.radius = ConfigKeys.getLandingZoneRadius();
        this.particle = parseParticle(ConfigKeys.getLandingZoneParticle());
    }

    @Override
    public void run() {
        tick++;
        double angleStep = Math.PI * 2 / 60; // 60 points around the circle
        double y = center.getY();

        for (int i = 0; i < 60; i++) {
            double angle = i * angleStep;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location point = new Location(world, x, y, z);

            // Pulse effect — particles alternate brightness
            if (tick % 2 == 0) {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0.01);
            } else {
                world.spawnParticle(particle, point, 1, 0, 0, 0, 0.01);
            }
        }

        // Center marker — vertical column
        if (tick % 4 == 0) {
            for (double dy = 0; dy < 3; dy += 0.5) {
                Location columnPoint = center.clone().add(0, dy, 0);
                world.spawnParticle(particle, columnPoint, 1, 0, 0, 0, 0.01);
            }
        }
    }

    /**
     * Start this task. Returns the task handle for cancellation.
     */
    public static RenderLandingZoneTask start(Location center, World world, JavaPlugin plugin) {
        RenderLandingZoneTask task = new RenderLandingZoneTask(center, world);
        task.runTaskTimer(plugin, 0L, 5L); // every 5 ticks = 4 times per second
        return task;
    }

    private Particle parseParticle(String name) {
        try {
            return Particle.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Particle.FLAME;
        }
    }
}
