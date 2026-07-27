package com.supplydrop.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RenderPackageLandedTask extends BukkitRunnable {

    private final Location location;
    private final World world;
    private int ticksElapsed = 0;
    private static final int MAX_TICKS = 20;

    public RenderPackageLandedTask(Location location, World world) {
        this.location = location.clone().add(0.5, 0.5, 0.5);
        this.world = world;
    }

    @Override
    public void run() {
        if (ticksElapsed >= MAX_TICKS) {
            this.cancel();
            return;
        }

        double progress = (double) ticksElapsed / MAX_TICKS;
        double height = progress * 2.0;

        world.spawnParticle(Particle.GLOW,
                location.clone().add(0, height, 0),
                15, 0.3, 0.1, 0.3, 0.05);

        double radius = 1.5 * (1 - progress);
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            double x = Math.cos(angle) * radius;
            double y = Math.sin(ticksElapsed * 0.5) * 0.1;
            double z = Math.sin(angle) * radius;
            world.spawnParticle(Particle.END_ROD, location.clone().add(x, y, z), 1, 0, 0, 0, 0);
        }

        ticksElapsed++;
    }
}
