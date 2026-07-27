package com.supplydrop.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class RenderFlareTask extends BukkitRunnable {

    private final Location location;
    private final World world;
    private boolean shouldContinue = true;
    private int ticksElapsed = 0;
    private static final int MAX_TICKS = 200;

    public RenderFlareTask(Location location, World world) {
        this.location = location.getBlock().getLocation().add(0.5, 0.0, 0.5);
        this.world = world;
    }

    @Override
    public void run() {
        if (!shouldContinue || ticksElapsed >= MAX_TICKS) {
            this.cancel();
            return;
        }

        for (int i = 0; i < 3; i++) {
            double xOffset = (Math.random() - 0.5) * 0.2;
            double zOffset = (Math.random() - 0.5) * 0.2;
            Location flareLoc = location.clone().add(xOffset, 0.1, zOffset);
            world.spawnParticle(Particle.DUST, flareLoc, 0, 0, 0, 0, 0,
                    new Particle.DustOptions(org.bukkit.Color.RED, 2.0f));
            world.spawnParticle(Particle.FLAME, flareLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (ticksElapsed % 2 == 0) {
            Location smokeLoc = location.clone().add(0, 0.2, 0);
            world.spawnParticle(Particle.SMOKE, smokeLoc, 4, 0.1, 0, 0.1, 0.02);
            world.spawnParticle(Particle.LARGE_SMOKE, smokeLoc, 2, 0.05, 0.1, 0.05, 0.05);
            if (ticksElapsed % 4 == 0) {
                world.playSound(location, Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.2f);
            } else if (ticksElapsed % 6 == 0) {
                world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.24f, 1.8f);
            }
        }

        if (ticksElapsed % 5 == 0) {
            world.spawnParticle(Particle.LAVA, location.clone().add(0, 0.15, 0), 1, 0.1, 0, 0.1, 0);
        }

        ticksElapsed++;
    }
}
