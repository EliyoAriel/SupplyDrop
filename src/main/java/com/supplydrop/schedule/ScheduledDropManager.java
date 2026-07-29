package com.supplydrop.schedule;

import com.supplydrop.SupplyDrop;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ScheduledDropManager {

    private final SupplyDrop plugin;
    private final Random random = new Random();
    private BukkitTask checkTask;
    private final Set<String> firedToday = new CopyOnWriteArraySet<>();

    public ScheduledDropManager(SupplyDrop plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!ConfigKeys.isAutoDropScheduledEnabled()) return;
        firedToday.clear();
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkScheduled, 0L, 20L);
        AirdropLogger.info("Scheduled drop manager started.");
    }

    public void stop() {
        if (checkTask != null && !checkTask.isCancelled()) {
            checkTask.cancel();
        }
        checkTask = null;
        firedToday.clear();
    }

    private void checkScheduled() {
        List<String> times = ConfigKeys.getAutoDropScheduledTimes();
        if (times.isEmpty()) return;

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        if (!times.contains(nowStr)) return;
        if (firedToday.contains(nowStr)) return;

        firedToday.add(nowStr);

        String worldName = ConfigKeys.getAutoDropScheduledWorldOverride();
        if (worldName == null || worldName.isEmpty()) {
            worldName = ConfigKeys.getAutoDropWorld();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            AirdropLogger.warning("Scheduled drop: world '" + worldName + "' not found.");
            return;
        }

        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.isEmpty()) return;

        int totalWeight = templates.stream().mapToInt(TemplateWeight::weight).sum();
        if (totalWeight <= 0) return;

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        String selectedName = templates.get(0).name();
        for (TemplateWeight tw : templates) {
            cumulative += tw.weight();
            if (roll < cumulative) { selectedName = tw.name(); break; }
        }

        Package pkg = PackageManager.get(selectedName);
        if (pkg == null) return;

        int radius = ConfigKeys.getAutoDropRandomRadius();
        int centerX = world.getSpawnLocation().getBlockX();
        int centerZ = world.getSpawnLocation().getBlockZ();
        int x = centerX + random.nextInt(-radius, radius + 1);
        int z = centerZ + random.nextInt(-radius, radius + 1);
        Location loc = new Location(world, x + 0.5, 0, z + 0.5);

        int onlineCount = world.getPlayers().size();
        int waveCount = ConfigKeys.getAutoDropWaveCountForPlayers(onlineCount);
        int playerBonusRolls = ConfigKeys.getAutoDropBonusRollsForPlayers(onlineCount);

        try {
            if (waveCount > 1) {
                DropController.dropWave(pkg, world, loc, waveCount, playerBonusRolls);
            } else {
                DropController.dropAtLocation(pkg, world, loc, playerBonusRolls);
            }
            DropController.markDropCompleted();
            AirdropLogger.info("Scheduled drop fired: " + selectedName + " at " + loc.getBlockX() + ", " + loc.getBlockZ());
        } catch (SkyNotClearException e) {
            AirdropLogger.warning("Scheduled drop failed: sky not clear.");
        }
    }

    public List<String> getScheduledTimes() {
        return new ArrayList<>(ConfigKeys.getAutoDropScheduledTimes());
    }
}
