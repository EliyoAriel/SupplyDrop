package com.supplydrop;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.ChatTheme;
import com.supplydrop.helpers.NotificationManager;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;

public class AutoDropScheduler {

    private final SupplyDrop plugin;
    private BukkitTask schedulerTask;
    private final Random random = new Random();
    private boolean running = false;

    public AutoDropScheduler(SupplyDrop plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (running) return;

        if (!ConfigKeys.isAutoDropEnabled()) {
            AirdropLogger.debug("Auto-drop is disabled in config.");
            return;
        }

        if (ConfigKeys.isAutoDropPaused()) {
            AirdropLogger.info("Auto-drop scheduler is paused.");
            return;
        }

        scheduleNextDrop();
        running = true;
        AirdropLogger.info("Auto-drop scheduler started.");
    }

    public void stop() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
        }
        schedulerTask = null;
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    private void scheduleNextDrop() {
        int interval;
        if (ConfigKeys.isAutoDropRandomInterval()) {
            int min = ConfigKeys.getAutoDropIntervalMin();
            int max = ConfigKeys.getAutoDropIntervalMax();
            if (min > 0 && max > min) {
                interval = min + random.nextInt(Math.max(1, max - min + 1));
            } else {
                interval = ConfigKeys.getAutoDropInterval();
            }
        } else {
            interval = ConfigKeys.getAutoDropInterval();
        }

        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::executeDrop, interval, interval);
        AirdropLogger.debug("Next auto-drop in " + interval + " ticks");
    }

    private void executeDrop() {
        if (ConfigKeys.isAutoDropPaused()) {
            AirdropLogger.debug("Auto-drop paused, skipping this tick.");
            return;
        }

        String worldName = ConfigKeys.getAutoDropWorld();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            AirdropLogger.warning("Auto-drop world '" + worldName + "' not found.");
            return;
        }

        Package pkg = resolvePackage();
        if (pkg == null) return;

        int radius = ConfigKeys.getAutoDropRandomRadius();
        Location spawnLoc = getRandomLocation(world, radius);

        // Announce
        if (ConfigKeys.isAnnouncementEnabled() && ConfigKeys.isAutoDropAnnounce()) {
            int delay = ConfigKeys.getAutoDropAnnounceDelay();
            boolean actionbar = ConfigKeys.isAutoDropAnnounceActionbar();
            int coordDelay = ConfigKeys.getAutoDropCoordRevealDelay();

            if (actionbar) {
                // Actionbar: show immediately, no coords
                Component actionbarMsg = Component.text("§e§lSupply Drop §b" + pkg.getDisplayName() + " §eincoming!");
                for (Player p : world.getPlayers()) {
                    p.sendActionBar(actionbarMsg);
                }
            } else {
                // Chat announcement via NotificationManager
                if (coordDelay > 0) {
                    // Announce without coords first
                    String noLocMsg = "&bSupply drop &e" + pkg.getDisplayName() + " &bincoming! Coordinates revealed in &e" + formatTime(coordDelay) + "&b!";
                    NotificationManager.notify(worldName, noLocMsg);
                    // Reveal coords after delay
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        String coordMsg = "&bSupply drop &e" + pkg.getDisplayName() + " &bat &e" +
                                spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ() + "&b!";
                        NotificationManager.notify(worldName, coordMsg);
                    }, coordDelay);
                } else {
                    // Announce with coords immediately
                    String msg = "&bSupply drop &e" + pkg.getDisplayName() + " &bincoming at &e" +
                            spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ() + "&b!";
                    NotificationManager.notify(worldName, msg);
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeDropAtLocation(pkg, world, spawnLoc);
            }, delay);
        } else {
            executeDropAtLocation(pkg, world, spawnLoc);
        }
    }

    private void executeDropAtLocation(Package pkg, World world, Location spawnLoc) {
        int waveCount = ConfigKeys.getAutoDropWaveCount();

        try {
            if (waveCount > 1) {
                DropController.dropWave(pkg, world, spawnLoc, waveCount);
                AirdropLogger.info("Wave drop: " + waveCount + " crates at " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ());
            } else {
                DropController.dropAtLocation(pkg, world, spawnLoc);
            }
            DropController.markDropCompleted();
        } catch (SkyNotClearException e) {
            AirdropLogger.warning("Auto-drop failed: sky not clear at target location.");
        }
    }

    private Package resolvePackage() {
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.isEmpty()) {
            AirdropLogger.warning("No auto-drop templates configured.");
            return null;
        }

        // Weighted random selection
        int totalWeight = templates.stream().mapToInt(TemplateWeight::weight).sum();
        if (totalWeight <= 0) {
            AirdropLogger.warning("All auto-drop template weights are zero.");
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        String selectedName = templates.get(0).name();

        for (TemplateWeight tw : templates) {
            cumulative += tw.weight();
            if (roll < cumulative) {
                selectedName = tw.name();
                break;
            }
        }

        Package pkg = PackageManager.get(selectedName);
        if (pkg == null) {
            AirdropLogger.warning("Auto-drop template '" + selectedName + "' not found.");
        }
        return pkg;
    }

    private Location getRandomLocation(World world, int radius) {
        int centerX = world.getSpawnLocation().getBlockX();
        int centerZ = world.getSpawnLocation().getBlockZ();
        int x = centerX + random.nextInt(-radius, radius + 1);
        int z = centerZ + random.nextInt(-radius, radius + 1);
        return new Location(world, x + 0.5, 0, z + 0.5);
    }

    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) return seconds + "s";
        int minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) return minutes + "m " + seconds + "s";
        int hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }
}
