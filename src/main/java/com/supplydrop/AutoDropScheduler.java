package com.supplydrop;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.config.DropOptions;
import com.supplydrop.controllers.DropController;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.ChatTheme;
import com.supplydrop.helpers.NotificationManager;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;
import com.supplydrop.announce.AnnouncementManager;
import com.supplydrop.chain.ChainManager;
import com.supplydrop.seasons.Season;
import com.supplydrop.stats.AutoDropStats;
import com.supplydrop.warning.DropWarning;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class AutoDropScheduler {

    public record QueuedDrop(String templateName, int waveCount, Location location, long queuedAt) {}

    private final SupplyDrop plugin;
    private final ChainManager chainManager;
    private final DropWarning dropWarning;
    private BukkitTask schedulerTask;
    private BukkitTask pollTask;
    private final Deque<QueuedDrop> queue = new ArrayDeque<>();
    private final Random random = new Random();
    private boolean running = false;

    public AutoDropScheduler(SupplyDrop plugin) {
        this.plugin = plugin;
        this.chainManager = new ChainManager();
        this.dropWarning = new DropWarning();
    }

    public ChainManager getChainManager() {
        return chainManager;
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
        startQueuePoll();
        running = true;
        AirdropLogger.info("Auto-drop scheduler started.");
    }

    public void stop() {
        if (schedulerTask != null && !schedulerTask.isCancelled()) {
            schedulerTask.cancel();
        }
        schedulerTask = null;
        stopQueuePoll();
        queue.clear();
        dropWarning.cancelAll();
        running = false;
    }

    private void startQueuePoll() {
        if (!ConfigKeys.isAutoDropQueueEnabled()) return;
        int interval = ConfigKeys.getAutoDropQueuePollInterval();
        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pollQueue, interval, interval);
    }

    private void stopQueuePoll() {
        if (pollTask != null && !pollTask.isCancelled()) {
            pollTask.cancel();
        }
        pollTask = null;
    }

    private void pollQueue() {
        if (queue.isEmpty()) return;

        QueuedDrop queued = queue.peek();
        if (queued == null) return;

        Package pkg = PackageManager.get(queued.templateName());
        if (pkg == null) {
            queue.poll();
            return;
        }

        World world = (World) queued.location().getWorld();
        if (world == null) {
            queue.poll();
            return;
        }

        try {
            DropController.getSpawnLocation(world, queued.location(),
                    DropOptions.createDefault());
            queue.poll();
            int onlineCount = world.getPlayers().size();
            int playerBonusRolls = ConfigKeys.getAutoDropBonusRollsForPlayers(onlineCount);

            if (queued.waveCount() > 1) {
                DropController.dropWave(pkg, world, queued.location(), queued.waveCount(), playerBonusRolls);
            } else {
                DropController.dropAtLocation(pkg, world, queued.location(), playerBonusRolls);
            }
            DropController.markDropCompleted();
            AirdropLogger.info("Queued drop fired: " + queued.templateName() + " at " +
                    queued.location().getBlockX() + ", " + queued.location().getBlockZ());
        } catch (SkyNotClearException e) {
            // Still not clear, leave in queue
        }
    }

    public void addToQueue(String templateName, int waveCount, Location location) {
        int maxSize = ConfigKeys.getAutoDropQueueMaxSize();
        if (queue.size() >= maxSize) {
            queue.pollLast();
        }
        queue.addFirst(new QueuedDrop(templateName, waveCount, location, System.currentTimeMillis()));
    }

    public boolean removeFromQueue(String templateName) {
        return queue.removeIf(q -> q.templateName().equalsIgnoreCase(templateName));
    }

    public List<QueuedDrop> getQueuedDrops() {
        return List.copyOf(queue);
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getQueueMaxSize() {
        return ConfigKeys.getAutoDropQueueMaxSize();
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

        // Try chain trigger — chain drops bypass queue
        if (chainManager.tryTriggerChain(world, spawnLoc)) {
            return;
        }

        int announceDelay = ConfigKeys.isAnnouncementEnabled() && ConfigKeys.isAutoDropAnnounce()
                ? ConfigKeys.getAutoDropAnnounceDelay() : 0;

        // Schedule warning countdown
        if (ConfigKeys.isAutoDropWarningEnabled()) {
            dropWarning.schedule(world, pkg.getDisplayName(), 1, announceDelay);
        }

        // Announce via AnnouncementManager
        if (announceDelay > 0) {
            String seasonPrefix = getActiveSeasonPrefix();
            AnnouncementManager.announce("normal", pkg.getDisplayName(), 1,
                    seasonPrefix, spawnLoc, world, worldName);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                executeDropAtLocation(pkg, world, spawnLoc);
            }, announceDelay);
        } else {
            executeDropAtLocation(pkg, world, spawnLoc);
        }
    }

    private void executeDropAtLocation(Package pkg, World world, Location spawnLoc) {
        int onlineCount = world.getPlayers().size();
        int waveCount = ConfigKeys.getAutoDropWaveCountForPlayers(onlineCount);
        int playerBonusRolls = ConfigKeys.getAutoDropBonusRollsForPlayers(onlineCount);

        try {
            int crateCount;
            if (waveCount > 1) {
                java.util.List<Crate> crates = DropController.dropWave(pkg, world, spawnLoc, waveCount, playerBonusRolls);
                crateCount = crates != null ? crates.size() : 0;
                AirdropLogger.info("Wave drop: " + waveCount + " crates at " + spawnLoc.getBlockX() + ", " + spawnLoc.getBlockZ());
            } else {
                crateCount = DropController.dropAtLocation(pkg, world, spawnLoc, playerBonusRolls);
            }
            DropController.markDropCompleted();

            // Record stats
            AutoDropStats stats = AutoDropStats.get();
            if (stats != null) {
                stats.incrementDrop();
                stats.incrementCrates(crateCount);
                stats.incrementTemplate(pkg.getName());
                stats.setLastDropTime(System.currentTimeMillis());
            }

            incrementEscalation();
        } catch (SkyNotClearException e) {
            if (ConfigKeys.isAutoDropQueueEnabled()) {
                addToQueue(pkg.getName(), waveCount, spawnLoc);
                AirdropLogger.info("Sky not clear, queued drop: " + pkg.getName() + " (queue: " + queue.size() + ")");
            } else {
                AirdropLogger.warning("Auto-drop failed: sky not clear at target location.");
            }
        }
    }

    private void incrementEscalation() {
        if (!ConfigKeys.isAutoDropEscalationEnabled()) return;
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return;
        int level = config.getEscalationLevel();
        config.setEscalationLevel(level + 1);
    }

    private Package resolvePackage() {
        Season activeSeason = plugin.getSeasonManager() != null ? plugin.getSeasonManager().getActiveSeason() : null;
        List<TemplateWeight> templates;

        if (activeSeason != null && !activeSeason.templates().isEmpty()) {
            templates = activeSeason.templates();
            AirdropLogger.debug("Using season '" + activeSeason.name() + "' templates");
        } else {
            templates = ConfigKeys.getAutoDropTemplates();
        }

        if (templates.isEmpty()) {
            AirdropLogger.warning("No auto-drop templates configured.");
            return null;
        }

        String selectedName;

        if (ConfigKeys.isAutoDropRotationEnabled()) {
            selectedName = resolveWithRotation(templates);
        } else {
            // Weighted random selection
            int totalWeight = templates.stream().mapToInt(TemplateWeight::weight).sum();
            if (totalWeight <= 0) {
                AirdropLogger.warning("All auto-drop template weights are zero.");
                return null;
            }

            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            selectedName = templates.get(0).name();

            for (TemplateWeight tw : templates) {
                cumulative += tw.weight();
                if (roll < cumulative) {
                    selectedName = tw.name();
                    break;
                }
            }
        }

        Package pkg = PackageManager.get(selectedName);
        if (pkg == null) {
            AirdropLogger.warning("Auto-drop template '" + selectedName + "' not found.");
        }
        return pkg;
    }

    private String resolveWithRotation(List<TemplateWeight> templates) {
        Config config = SupplyDrop.getConfiguration();
        if (config == null) return templates.get(0).name();

        int index = config.getRotationIndex();
        if (index < 0 || index >= templates.size()) {
            index = 0;
        }

        String selected = templates.get(index).name();

        int nextIndex = (index + 1) % templates.size();
        config.setRotationIndex(nextIndex);

        return selected;
    }

    public String getActiveSeasonPrefix() {
        Season activeSeason = plugin.getSeasonManager() != null ? plugin.getSeasonManager().getActiveSeason() : null;
        return activeSeason != null ? activeSeason.announcePrefix() : "";
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
