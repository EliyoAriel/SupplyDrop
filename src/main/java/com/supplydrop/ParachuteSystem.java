package com.supplydrop;

import java.util.ArrayList;

import com.supplydrop.config.DropOptions;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class ParachuteSystem {

    private final World world;
    private final ArrayList<Chicken> chickenParachutes;
    private final DropOptions options;
    private Slime parachuteLeash;
    private FallingBlock fallingCrate;
    private BukkitTask parachuteTask;
    private double fallVelocity;
    private BukkitTask delayedCleanupTask;
    private SupplyDrop plugin;
    private boolean parachutesReleased;

    public ParachuteSystem(World world, DropOptions options) {
        this.world = world;
        this.chickenParachutes = new ArrayList<>();
        this.options = options;
    }

    public void initialize(Location dropLocation, FallingBlock fallingCrate, SupplyDrop plugin) {
        this.fallingCrate = fallingCrate;
        this.plugin = plugin;
        this.parachutesReleased = false;
        cancelDelayedCleanupTask();

        Location leashLocation = dropLocation.clone().add(new Vector(0, 1, 0));
        parachuteLeash = (Slime) world.spawnEntity(leashLocation, EntityType.SLIME);
        parachuteLeash.setAI(false);
        parachuteLeash.setSize(1);
        parachuteLeash.setInvisible(true);
        parachuteLeash.setInvulnerable(true);

        for (int i = 0; i < options.getChickenCount(); i++) {
            Location chickenLocation = dropLocation.clone()
                    .add(new Vector(Math.random() * 0.25, 2 + i, Math.random() * 0.25));
            Chicken chicken = (Chicken) world.spawnEntity(chickenLocation, EntityType.CHICKEN);
            chicken.setInvulnerable(true);
            chicken.setLeashHolder(parachuteLeash);
            chickenParachutes.add(chicken);
        }

        fallingCrate.addPassenger(parachuteLeash);
        fallingCrate.setGravity(false);

        // Calculate fall velocity
        int fallDuration = options.getFallDuration();
        if (fallDuration > 0) {
            int groundY = world.getHighestBlockYAt(dropLocation);
            double height = dropLocation.getY() - groundY;
            // velocity = height / (duration_seconds * 10) — task runs every 2 ticks (10 times/sec)
            this.fallVelocity = height / (fallDuration * 10.0);
        } else {
            this.fallVelocity = options.getFallingSpeed();
        }

        startParachuteTask();
    }

    private void startParachuteTask() {
        if (plugin == null || !plugin.isEnabled()) return;

        parachuteTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (fallingCrate == null || fallingCrate.isDead()) {
                releaseParachutes();
                return;
            }

            Location effectLoc = fallingCrate.getLocation().add(new Vector(0, 1, 0));
            for (int i = 0; i < 3; i++) {
                fallingCrate.getWorld().playEffect(effectLoc, Effect.SMOKE, 0);
            }

            fallingCrate.setVelocity(new Vector(0, -fallVelocity, 0));
        }, 0, 2);
    }

    private void releaseParachutes() {
        for (Chicken chicken : chickenParachutes) {
            if (chicken == null || chicken.isDead()) continue;
            chicken.setLeashHolder(null);
            double xVel = Math.random() < 0.5 ? Math.random() * 0.5 * -1 : Math.random() * 0.5;
            double zVel = Math.random() < 0.5 ? Math.random() * 0.5 * -1 : Math.random() * 0.5;
            chicken.setVelocity(new Vector(xVel, 0.5, zVel));
        }

        if (parachutesReleased) return;
        parachutesReleased = true;

        if (plugin == null || !plugin.isEnabled()) {
            cleanupParachuteEntities();
            return;
        }

        delayedCleanupTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            delayedCleanupTask = null;
            cancelParachuteTask();
            cleanupParachuteEntities();
        }, 60);
    }

    public void cancel() {
        parachutesReleased = true;
        if (parachuteTask != null && !parachuteTask.isCancelled()) {
            parachuteTask.cancel();
        }
        parachuteTask = null;
        cancelDelayedCleanupTask();
        cleanupParachuteEntities();
    }

    private void cleanupParachuteEntities() {
        for (Chicken chicken : chickenParachutes) {
            if (chicken != null && chicken.isValid() && !chicken.isDead()) {
                chicken.remove();
            }
        }
        chickenParachutes.clear();
        if (parachuteLeash != null && parachuteLeash.isValid() && !parachuteLeash.isDead()) {
            parachuteLeash.remove();
        }
    }

    private void cancelParachuteTask() {
        if (parachuteTask != null && !parachuteTask.isCancelled()) {
            parachuteTask.cancel();
        }
        parachuteTask = null;
    }

    private void cancelDelayedCleanupTask() {
        if (delayedCleanupTask != null && !delayedCleanupTask.isCancelled()) {
            delayedCleanupTask.cancel();
        }
        delayedCleanupTask = null;
    }
}
