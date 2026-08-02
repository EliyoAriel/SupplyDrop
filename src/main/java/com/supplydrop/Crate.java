package com.supplydrop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.DropOptions;
import com.supplydrop.hologram.Hologram;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.DatabaseManager;
import com.supplydrop.helpers.RewindIntegration;

import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;
import com.supplydrop.helpers.HistoryManager;
import com.supplydrop.tasks.RenderFlareTask;
import com.supplydrop.tasks.RenderLandingZoneTask;
import com.supplydrop.tasks.RenderPackageGlowTask;
import com.supplydrop.tasks.RenderPackageLandedTask;
import com.supplydrop.tasks.RenderPackageSmokeTask;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class Crate {

    public enum State {
        SPAWN,
        FALL,
        LAND,
        LOCK,
        READY_TO_OPEN
    }

    private UUID id;
    private final World world;
    private final ArrayList<ItemStack> contents;
    private State state;
    private final DropOptions options;
    private final String displayName;
    private final boolean trapCrate;

    private Location dropLocation;
    private FallingBlock fallingCrate;
    private ParachuteSystem parachuteSystem;

    private Location landedLocation;
    private Block blockChest;
    private BukkitTask glowTask;
    private BukkitTask smokeTask;
    private BukkitTask expiryTask;
    private BukkitTask hologramUpdateTask;
    private RenderFlareTask flareEffect;
    private RenderLandingZoneTask landingZoneTask;
    private Hologram hologram;
    private volatile boolean opened = false;
    private volatile boolean destroyed = false;

    // Expiry tracking
    private long landTime;
    private int expiryTicks;

    // Lock phase tracking
    private int lockDuration;
    private long lockStartTime;
    private BukkitTask lockTask;
    private BukkitTask lockParticleTask;
    private int lockDurationOverride = -1; // -1 = use config, >=0 = override

    // Team crate tracking
    private final Set<UUID> rightClickers = new HashSet<>();
    private int requiredPlayers;
    private boolean teamCrateEnabled;

    public Crate(Location location, World world, List<ItemStack> contents, DropOptions options, String displayName) {
        this(location, world, contents, options, displayName, false);
    }

    public Crate(Location location, World world, List<ItemStack> contents, DropOptions options, String displayName, boolean trapCrate) {
        this.id = UUID.randomUUID();
        this.dropLocation = location.clone();
        this.world = world;
        this.contents = cloneContents(contents);
        this.state = State.SPAWN;
        this.options = options;
        this.displayName = displayName;
        this.trapCrate = trapCrate;
        this.parachuteSystem = new ParachuteSystem(world, options);

        // Team crate: roll chance (options override or crate.* global)
        int teamChance = options.getTeamCrateChance() != null ? options.getTeamCrateChance() : ConfigKeys.getCrateTeamOpenChance();
        this.teamCrateEnabled = teamChance > 0 && new java.util.Random().nextInt(100) < teamChance;
        this.requiredPlayers = options.getTeamCrateRange() != null ? options.getTeamCrateRange() : ConfigKeys.getCrateTeamOpenPlayers();
    }

    private static ArrayList<ItemStack> cloneContents(List<ItemStack> contents) {
        ArrayList<ItemStack> cloned = new ArrayList<>();
        if (contents == null) return cloned;
        for (ItemStack content : contents) {
            if (content != null) cloned.add(content.clone());
        }
        return cloned;
    }

    // ─── LIFECYCLE ──────────────────────────────────────────────────

    public void dropCrate() {
        if (state != State.SPAWN) {
            throw new IllegalStateException("Cannot drop a crate that is not in SPAWN state");
        }
        SupplyDrop plugin = getEnabledPlugin();
        if (plugin == null) {
            throw new IllegalStateException("Cannot drop crate while plugin is unavailable");
        }

        Location groundLocation = dropLocation.clone();
        groundLocation.setY(dropLocation.getY() - options.getDropHeight() + 1);
        if (options.shouldShowFlareEffects()) {
            flareEffect = new RenderFlareTask(groundLocation, world);
            flareEffect.runTaskTimer(plugin, 0L, 1L);
        }

        fallingCrate = world.spawn(dropLocation, FallingBlock.class, fb -> {
            fb.setBlockData(Material.BARREL.createBlockData());
        });

        parachuteSystem.initialize(dropLocation, fallingCrate, plugin);
        CrateManager.addCrate(fallingCrate, this);
        this.state = State.FALL;

        // Persist to database immediately (updated again on land)
        DatabaseManager.saveWithState(dropLocation, displayName, trapCrate, teamCrateEnabled, requiredPlayers, id.toString(),
                State.FALL.name(), lockDuration, 0, options.getExpiryTicks(), 0);

        // Log spawn event
        HistoryManager.logEvent("SPAWN", displayName, dropLocation, null,
                trapCrate ? "trap" : (teamCrateEnabled ? "team:" + requiredPlayers : "normal"));

        // Start landing zone marker
        if (ConfigKeys.isLandingZoneEnabled()) {
            int bx = dropLocation.getBlockX();
            int bz = dropLocation.getBlockZ();
            int groundY = world.getHighestBlockYAt(bx, bz) + 1;
            Location groundLoc = new Location(world, bx + 0.5, groundY, bz + 0.5);
            landingZoneTask = RenderLandingZoneTask.start(groundLoc, world, plugin);
        }
    }

    public void land(Block block) {
        if (state != State.FALL) {
            throw new IllegalStateException("Cannot land a crate that is not in FALL state");
        }

        this.blockChest = block;
        this.landedLocation = block.getLocation().clone();
        this.state = State.LAND;

        blockChest.setType(Material.BARREL);
        BlockState barrelState = blockChest.getState();
        if (!(barrelState instanceof Barrel barrel)) {
            throw new IllegalStateException("Failed to create barrel at landed location");
        }

        if (displayName != null && !displayName.isEmpty()) {
            barrel.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
            barrel.update();
        }

        com.supplydrop.integration.LandClaimHook.tagCrate(barrel.getBlock(), id);

        int overflowCount = 0;
        for (ItemStack is : contents) {
            Map<Integer, ItemStack> overflow = barrel.getInventory().addItem(is);
            for (ItemStack remaining : overflow.values()) {
                if (remaining != null && !remaining.getType().isAir()) {
                    overflowCount++;
                    org.bukkit.entity.Item itemEntity = world.dropItemNaturally(landedLocation.clone().add(0.5, 0.5, 0.5), remaining);
                    com.supplydrop.integration.LandClaimHook.tagCrateLoot(itemEntity);
                }
            }
        }
        if (overflowCount > 0) {
            AirdropLogger.warning("Dropped " + overflowCount + " overflow item stack(s) at landed crate");
        }

        CrateManager.addCrate(barrel.getLocation(), this);
        DatabaseManager.saveWithState(landedLocation, displayName, trapCrate, teamCrateEnabled, requiredPlayers, id.toString(),
                State.LAND.name(), lockDuration, 0, options.getExpiryTicks(), System.currentTimeMillis());
        SupplyDrop plugin = getEnabledPlugin();

        if (plugin != null && options.shouldShowLandingEffects()) {
            new RenderPackageLandedTask(landedLocation.clone(), world).runTask(plugin);
        }

        if (plugin != null && options.shouldShowContinuousEffects()) {
            RenderPackageGlowTask glowEffect = new RenderPackageGlowTask(landedLocation.clone(), world);
            this.glowTask = glowEffect.runTaskTimer(plugin, 0L, 10L);
        }

        if (plugin != null && options.isSmokeEnabled()) {
            RenderPackageSmokeTask smokeEffect = new RenderPackageSmokeTask(landedLocation.clone(), world, options.getSmokeHeight());
            this.smokeTask = smokeEffect.runTaskTimer(plugin, 0L, 100L);
        }

        world.playSound(landedLocation, Sound.ENTITY_PLAYER_LEVELUP, 0.05f, 0.05f);

        // Cancel flare
        if (flareEffect != null) {
            flareEffect.cancel();
        }

        // Cancel landing zone marker
        if (landingZoneTask != null) {
            landingZoneTask.cancel();
            landingZoneTask = null;
        }

        // Transition to LOCK or READY
        startLock();

        // Notify Rewind to exclude chunks around this crate
        RewindIntegration.onCrateLand(this);
    }

    // ─── LOCK PHASE ─────────────────────────────────────────────────

    private void startLock() {
        SupplyDrop plugin = getEnabledPlugin();

        // Compute lock duration
        if (lockDurationOverride >= 0) {
            lockDuration = lockDurationOverride;
        } else if (!ConfigKeys.isCrateLockEnabled()) {
            lockDuration = 0;
        } else {
            // Per-template override
            Package pkg = PackageManager.get(displayName);
            int templateLock = pkg != null ? pkg.getLockDuration() : 0;
            if (templateLock > 0) {
                lockDuration = templateLock;
            } else if (ConfigKeys.isCrateLockRandom()) {
                int min = ConfigKeys.getCrateLockDurationMin();
                int max = ConfigKeys.getCrateLockDurationMax();
                lockDuration = min + new java.util.Random().nextInt(Math.max(1, max - min + 1));
            } else {
                lockDuration = ConfigKeys.getCrateLockDuration();
            }
        }

        if (lockDuration <= 0) {
            readyToOpen();
            return;
        }

        this.state = State.LOCK;
        this.lockStartTime = System.currentTimeMillis();

        // Persist lock state
        DatabaseManager.saveWithState(landedLocation, displayName, trapCrate, teamCrateEnabled, requiredPlayers, id.toString(),
                State.LOCK.name(), lockDuration, lockStartTime, options.getExpiryTicks(), 0);

        // Create hologram
        if (ConfigKeys.isHologramEnabled()) {
            hologram = new Hologram(landedLocation, buildHologramLines());
        }

        // Hologram update every second
        if (plugin != null) {
            hologramUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (state == State.LOCK && !opened && !destroyed && hologram != null && !hologram.isDead()) {
                    hologram.updateLines(buildHologramLines());
                }
            }, 20L, 20L);
        }

        // Lock particle effect
        if (ConfigKeys.isCrateLockParticleEnabled() && plugin != null) {
            lockParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (state == State.LOCK && !opened && !destroyed && landedLocation != null) {
                    try {
                        Particle particle = Particle.valueOf(ConfigKeys.getCrateLockParticleType());
                        double radius = ConfigKeys.getCrateLockParticleRadius();
                        world.spawnParticle(particle, landedLocation.clone().add(0.5, 1, 0.5),
                                5, radius, 0.5, radius, 0.01);
                    } catch (IllegalArgumentException ignored) {}
                }
            }, 0L, 10L);
        }

        // Play lock sound
        playSound(ConfigKeys.getCrateLockSoundLock());

        // Log LOCK event
        HistoryManager.logEvent("LOCK", displayName, landedLocation, null,
                "duration:" + lockDuration);

        // Schedule transition to READY
        if (plugin != null) {
            lockTask = Bukkit.getScheduler().runTaskLater(plugin, this::readyToOpen, lockDuration);
        }
    }

    private void readyToOpen() {
        if (state == State.READY_TO_OPEN || opened || destroyed) return;

        this.state = State.READY_TO_OPEN;

        // Cancel lock tasks
        if (lockTask != null) { lockTask.cancel(); lockTask = null; }
        if (lockParticleTask != null) { lockParticleTask.cancel(); lockParticleTask = null; }
        if (hologramUpdateTask != null) { hologramUpdateTask.cancel(); hologramUpdateTask = null; }

        // Set expiry timers BEFORE hologram update so countdown starts from now
        expiryTicks = options.getExpiryTicks();
        landTime = System.currentTimeMillis();

        // Persist ready state
        DatabaseManager.saveWithState(landedLocation, displayName, trapCrate, teamCrateEnabled, requiredPlayers, id.toString(),
                State.READY_TO_OPEN.name(), lockDuration, lockStartTime, options.getExpiryTicks(), landTime);

        // Play ready sound
        playSound(ConfigKeys.getCrateLockSoundReady());

        // Notify nearby players
        if (ConfigKeys.isCrateLockReadyNotification() && landedLocation != null) {
            int radius = ConfigKeys.getCrateLockReadyNotificationRadius();
            String name = displayName != null ? displayName : "Supply Crate";
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(landedLocation) <= radius) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&a&lSupply Crate &7" + name + " &ais now unlockable! &7@ &b"
                                    + landedLocation.getBlockX() + ", " + landedLocation.getBlockZ()));
                }
            }
        }

        // Update hologram to show team info (if team crate)
        refreshHologram();

        // Start expiry timer
        SupplyDrop plugin = getEnabledPlugin();
        if (expiryTicks > 0 && plugin != null) {
            expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == State.READY_TO_OPEN && !opened && !destroyed) {
                    HistoryManager.logEvent("EXPIRE", displayName, landedLocation, null, null);
                    destroy();
                    AirdropLogger.info("Crate expired and was destroyed (loot taken by server).");
                }
            }, expiryTicks);

            // Update hologram every second for expiry countdown
            hologramUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (state == State.READY_TO_OPEN && !opened && !destroyed && hologram != null && !hologram.isDead()) {
                    hologram.updateLines(buildHologramLines());
                }
            }, 20L, 20L);
        }

        // Log READY event
        HistoryManager.logEvent("READY", displayName, landedLocation, null, null);
    }

    // ─── OPENED ─────────────────────────────────────────────────────

    /**
     * Handle a player right-clicking the crate.
     * Returns true if the crate should open, false if it's blocked (team crate waiting).
     */
    public boolean handleRightClick(Player player) {
        if (!teamCrateEnabled) return true;

        rightClickers.add(player.getUniqueId());

        if (rightClickers.size() >= requiredPlayers) {
            return true;
        }

        int remaining = requiredPlayers - rightClickers.size();
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e&lSupply Crate &7- &fWaiting for &b" + remaining + " &fmore player(s) to right-click!"));

        refreshHologram();
        return false;
    }

    /**
     * Mark crate as opened. Removes hologram, stops effects, removes from DB.
     */
    public void setOpened(boolean opened) {
        this.opened = opened;
        if (opened) {
            stopEffects();
            removeHologram();
            if (landedLocation != null) {
                DatabaseManager.remove(landedLocation);
            }
        }
    }

    // ─── DESTROY VARIANTS ───────────────────────────────────────────

    /**
     * Base destroy: removes entity/block, stops effects, removes from DB.
     * Does NOT drop items. Used for expiry and cleanup.
     */
    public void destroy() {
        if (destroyed) return;
        destroyed = true;

        if (state == State.FALL && fallingCrate != null && !fallingCrate.isDead()) {
            fallingCrate.setGravity(true);
            fallingCrate.remove();
        }

        if (parachuteSystem != null) parachuteSystem.cancel();

        if (state == State.LAND || state == State.LOCK || state == State.READY_TO_OPEN) {
            stopEffects();
            removeHologram();
            if (landedLocation != null) {
                DatabaseManager.remove(landedLocation);
                CrateManager.removeCrate(landedLocation);
            }
            if (blockChest != null && blockChest.getType() == Material.BARREL) {
                blockChest.setType(Material.AIR);
            } else if (landedLocation != null && landedLocation.getBlock().getType() == Material.BARREL) {
                landedLocation.getBlock().setType(Material.AIR);
            }
        }

        if (flareEffect != null && !flareEffect.isCancelled()) {
            flareEffect.cancel();
        }

        if (landingZoneTask != null) {
            landingZoneTask.cancel();
            landingZoneTask = null;
        }

        RewindIntegration.onCrateDestroy(this);
    }

    /**
     * Destroy and drop all remaining barrel contents on the ground.
     * Used for: normal barrel break, explosion, burn.
     */
    public void destroyAndDropContents() {
        if (destroyed) return;
        dropAllContents();
        destroy();
    }

    /**
     * Destroy with no item drop (team crate broken manually — punishment).
     * Items are lost, taken by server.
     */
    public void destroyTeamCrate() {
        if (destroyed) return;
        destroy();
    }

    /**
     * Destroy with trap trigger (trap crate broken manually).
     * Fires trap, then destroys with no item drop.
     */
    public void destroyWithTrap() {
        if (destroyed) return;
        triggerTrap();
        destroy();
    }

    /**
     * Handle barrel break during LOCK state based on break-behavior config.
     */
    public void destroyDuringLock() {
        if (destroyed) return;
        String behavior = ConfigKeys.getCrateLockBreakBehavior();
        if ("drop".equalsIgnoreCase(behavior)) {
            destroyAndDropContents();
        } else {
            destroy();
        }
    }

    /**
     * Drop all remaining barrel contents on the ground.
     */
    public void dropAllContents() {
        if (landedLocation == null) return;
        Block block = landedLocation.getBlock();
        if (block.getType() != Material.BARREL) return;
        if (!(block.getState() instanceof Barrel barrel)) return;

        Location dropLoc = landedLocation.clone().add(0.5, 0.5, 0.5);
        for (ItemStack item : barrel.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                org.bukkit.entity.Item itemEntity = world.dropItemNaturally(dropLoc, item);
                com.supplydrop.integration.LandClaimHook.tagCrateLoot(itemEntity);
            }
        }
        barrel.getInventory().clear();
    }

    // ─── TRAP ───────────────────────────────────────────────────────

    public void triggerTrap() {
        if (!trapCrate || landedLocation == null) return;

        List<String> mobs = options.getTrapMobs() != null ? options.getTrapMobs() : ConfigKeys.getCrateTrapMobs();
        if (!mobs.isEmpty()) {
            java.util.Random rng = new java.util.Random();
            for (int i = 0; i < 3; i++) {
                String mobType = mobs.get(rng.nextInt(mobs.size()));
                try {
                    EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                    Location spawnLoc = landedLocation.clone().add(
                            (Math.random() - 0.5) * 3,
                            1,
                            (Math.random() - 0.5) * 3);
                    LivingEntity entity = (LivingEntity) world.spawnEntity(spawnLoc, entityType);
                    if (entity != null) {
                        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lTrap!"));
                        entity.setCustomNameVisible(true);
                    }
                } catch (IllegalArgumentException e) {
                    AirdropLogger.warning("Invalid trap mob type: " + mobType);
                }
            }
        }

        world.createExplosion(landedLocation, 2.0f, false);
    }

    // ─── HOLOGRAM ───────────────────────────────────────────────────

    private List<String> buildHologramLines() {
        List<String> templateLines = ConfigKeys.getHologramLines();
        List<String> lines = new ArrayList<>();

        for (String line : templateLines) {
            String processed = line
                    .replace("{template}", displayName != null ? displayName : "")
                    .replace("{name}", displayName != null ? displayName : "")
                    .replace("{x}", landedLocation != null ? String.valueOf(landedLocation.getBlockX()) : "0")
                    .replace("{z}", landedLocation != null ? String.valueOf(landedLocation.getBlockZ()) : "0");

            // State label
            processed = processed.replace("{state}", getStateLabel());

            // Lock placeholders
            if (state == State.LOCK) {
                long elapsedMs = System.currentTimeMillis() - lockStartTime;
                long totalMs = (long) lockDuration * 50;
                long remainingMs = totalMs - elapsedMs;
                if (remainingMs < 0) remainingMs = 0;
                processed = processed.replace("{lock-time}", formatTimeRemaining(remainingMs));
                processed = processed.replace("{lock-progress}", buildLockProgress(remainingMs, totalMs));
            } else {
                processed = processed.replace("{lock-time}", "");
                processed = processed.replace("{lock-progress}", "");
            }

            // Expiry placeholders
            if (state == State.READY_TO_OPEN && expiryTicks > 0) {
                long elapsedMs = System.currentTimeMillis() - landTime;
                long totalMs = (long) expiryTicks * 50;
                long remainingMs = totalMs - elapsedMs;
                if (remainingMs < 0) remainingMs = 0;
                processed = processed.replace("{time}", formatTimeRemaining(remainingMs));
            } else if (state == State.LOCK) {
                processed = processed.replace("{time}", "");
            } else {
                processed = processed.replace("{time}", "");
            }

            // Team placeholders
            if (teamCrateEnabled) {
                int remaining = requiredPlayers - rightClickers.size();
                if (remaining < 0) remaining = 0;
                processed = processed
                        .replace("{team}", "&e&lTEAM CRATE &7- &f" + requiredPlayers + " players needed")
                        .replace("{team-required}", String.valueOf(requiredPlayers))
                        .replace("{team-remaining}", String.valueOf(remaining))
                        .replace("{team-progress}", buildTeamProgress());
            } else {
                processed = processed
                        .replace("{team}", "")
                        .replace("{team-required}", "")
                        .replace("{team-remaining}", "")
                        .replace("{team-progress}", "");
            }

            // Escalation placeholder
            if (ConfigKeys.isAutoDropEscalationEnabled()) {
                Config cfg = SupplyDrop.getConfiguration();
                if (cfg != null) {
                    int level = cfg.getEscalationLevel();
                    int effectiveChance = Math.min(
                            ConfigKeys.getCrateTrapChance() + level * ConfigKeys.getAutoDropEscalationIncrement(),
                            ConfigKeys.getAutoDropEscalationCap());
                    processed = processed.replace("{escalation}", String.valueOf(effectiveChance));
                } else {
                    processed = processed.replace("{escalation}", "0");
                }
            } else {
                processed = processed.replace("{escalation}", "0");
            }

            lines.add(processed);
        }

        return lines;
    }

    private String getStateLabel() {
        return switch (state) {
            case SPAWN -> "&eSPAWNING";
            case FALL -> "&eFALLING";
            case LAND -> "&eLANDING";
            case LOCK -> "&cLOCKED";
            case READY_TO_OPEN -> "&aREADY";
        };
    }

    private String buildLockProgress(long remainingMs, long totalMs) {
        int totalBars = 10;
        double progress = totalMs > 0 ? 1.0 - ((double) remainingMs / totalMs) : 1.0;
        int filled = (int) Math.round(progress * totalBars);
        filled = Math.max(0, Math.min(totalBars, filled));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                sb.append("&a&l■ ");
            } else {
                sb.append("&7&l■ ");
            }
        }
        return sb.toString();
    }

    private String formatTimeRemaining(long ms) {
        long totalSeconds = ms / 1000;
        if (totalSeconds <= 0) return "&cExpired";

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return "&e" + hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return "&e" + minutes + "m " + seconds + "s";
        } else {
            return "&e" + seconds + "s";
        }
    }

    private String buildTeamProgress() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < requiredPlayers; i++) {
            if (i < rightClickers.size()) {
                sb.append("&a&l■ ");
            } else {
                sb.append("&7&l■ ");
            }
        }
        return sb.toString();
    }

    private void refreshHologram() {
        if (hologram != null && !hologram.isDead()) {
            hologram.updateLines(buildHologramLines());
        }
    }

    public void stopEffects() {
        if (glowTask != null) glowTask.cancel();
        if (smokeTask != null) smokeTask.cancel();
        if (expiryTask != null) expiryTask.cancel();
        if (hologramUpdateTask != null) hologramUpdateTask.cancel();
        if (lockTask != null) { lockTask.cancel(); lockTask = null; }
        if (lockParticleTask != null) { lockParticleTask.cancel(); lockParticleTask = null; }
    }

    private void removeHologram() {
        if (hologram != null) {
            hologram.destroy();
            hologram = null;
        }
    }

    private void playSound(String soundName) {
        if (landedLocation == null) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            world.playSound(landedLocation, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    // ─── GETTERS / SETTERS ──────────────────────────────────────────

    public UUID getId() { return id; }
    public String getShortId() { return id.toString().substring(0, 8); }
    public State getState() { return state; }
    public FallingBlock getFallingCrate() { return fallingCrate; }
    public Location getLocation() { return state == State.FALL ? dropLocation : landedLocation; }
    public Location getDropLocation() { return dropLocation; }
    public Location getLandedLocation() { return landedLocation; }
    public boolean getOpened() { return opened; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isTrapCrate() { return trapCrate; }
    public boolean isTeamCrate() { return teamCrateEnabled; }
    public int getRequiredPlayers() { return requiredPlayers; }
    public String getDisplayName() { return displayName; }
    public boolean isLocked() { return state == State.LOCK; }
    public int getLockDuration() { return lockDuration; }

    /**
     * Get remaining lock time in milliseconds, or -1 if not locked.
     */
    public long getLockRemainingMs() {
        if (state != State.LOCK) return -1;
        long totalMs = (long) lockDuration * 50;
        long elapsedMs = System.currentTimeMillis() - lockStartTime;
        long remaining = totalMs - elapsedMs;
        return Math.max(0, remaining);
    }

    /**
     * Set lock duration override. Pass -1 to use config, 0 to skip lock.
     */
    public void setLockDurationOverride(int override) {
        this.lockDurationOverride = override;
    }

    /**
     * Get list of player names who contributed to opening this team crate.
     */
    public List<String> getContributorNames() {
        List<String> names = new ArrayList<>();
        for (UUID uuid : rightClickers) {
            org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(uuid);
            names.add(player != null ? player.getName() : "Unknown");
        }
        return names;
    }

    public void forceTeamCrate(int requiredPlayers) {
        this.teamCrateEnabled = true;
        this.requiredPlayers = Math.max(2, requiredPlayers);
    }

    /**
     * Create a crate from persisted data (no contents, just tracking).
     */
    public static Crate createPersisted(UUID id, Location location, String displayName, boolean trapCrate,
                                        boolean isTeamCrate, int requiredPlayers) {
        Crate crate = new Crate(location, location.getWorld(), new ArrayList<>(),
                DropOptions.createDefault(), displayName, trapCrate);
        crate.id = id;
        crate.state = State.LAND;
        crate.landedLocation = location.clone();
        crate.blockChest = location.getBlock();
        crate.teamCrateEnabled = isTeamCrate;
        crate.requiredPlayers = requiredPlayers;
        return crate;
    }

    /**
     * Create a persisted crate in a specific state (for server restart resume).
     */
    public static Crate createPersistedInState(UUID id, Location location, String displayName, boolean trapCrate,
                                               boolean isTeamCrate, int requiredPlayers, State resumeState,
                                               int lockDuration, long lockStartTime, int expiryTicks, long landTime) {
        Crate crate = createPersisted(id, location, displayName, trapCrate, isTeamCrate, requiredPlayers);
        crate.state = resumeState;
        crate.lockDuration = lockDuration;
        crate.lockStartTime = lockStartTime;
        crate.expiryTicks = expiryTicks;
        crate.landTime = landTime;
        return crate;
    }

    private SupplyDrop getEnabledPlugin() {
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        return (plugin != null && plugin.isEnabled()) ? plugin : null;
    }
}
