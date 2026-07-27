package com.supplydrop.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class RenderPackageGlowTask extends BukkitRunnable {

    private final Location location;
    private final World world;

    public RenderPackageGlowTask(Location location, World world) {
        this.location = location.getBlock().getLocation().add(0.5, 1.0, 0.5);
        this.world = world;
    }

    @Override
    public void run() {
        world.spawnParticle(Particle.GLOW, location, 3, 0.3, 0.3, 0.3, 0.0);
    }
}
