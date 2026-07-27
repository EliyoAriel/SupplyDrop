package com.supplydrop.tasks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RenderPackageSmokeTask extends BukkitRunnable {

    private final Location location;
    private final World world;
    private final int smokeHeight;

    public RenderPackageSmokeTask(Location location, World world, int smokeHeight) {
        this.location = location.getBlock().getLocation().add(0.5, 1.0, 0.5);
        this.world = world;
        this.smokeHeight = smokeHeight;
    }

    @Override
    public void run() {
        Vector offset = new Vector(0, 1, 0);
        for (int i = 0; i < smokeHeight; i++) {
            Location newLocation = location.clone().add(offset.clone().multiply(i));
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, newLocation, 0, 0, 0, 0, 0, null, true);
        }
    }
}
