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
import com.supplydrop.tasks.RenderFlareTask;
import com.supplydrop.tasks.RenderLandingZoneTask;
import com.supplydrop.tasks.RenderPackageGlowTask;
import com.supplydrop.tasks.RenderPackageLandedTask;
import com.supplydrop.tasks.RenderPackageSmokeTask;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
        FALLING,
        LANDED
    }

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

    // Team crate tracking
    private final Set<UUID> rightClickers = new HashSet<>();
    private int requiredPlayers;
    private boolean teamCrateEnabled;

    public Crate(Location location, World world, List<ItemStack> contents, DropOptions options, String displayName) {
        this(location, world, contents, options, displayName, false);
    }

    public Crate(Location location, World world, List<ItemStack> contents, DropOptions options, String displayName, boolean trapCrate) {
        this.dropLocation = location.clone();
        this.world = world;
        this.contents = cloneContents(contents);
        this.state = State.FALLING;
        this.options = options;
        this.displayName = displayName;
        this.trapCrate = trapCrate;
        this.parachuteSystem = new ParachuteSystem(world, options);

        // Team crate: roll chance
        int teamChance = ConfigKeys.getCrateTeamOpenChance();
        this.teamCrateEnabled = teamChance > 0 && new java.util.Random().nextInt(100) < teamChance;
        this.requiredPlayers = ConfigKeys.getCrateTeamOpenPlayers();
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
        if (state != State.FALLING) {
            throw new IllegalStateException("Cannot drop a crate that is not in FALLING state");
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

        // Start landing zone marker
        if (ConfigKeys.isLandingZoneEnabled()) {
            int bx = dropLocation.getBlockX();
            int bz = dropLocation.getBlockZ();
            int groundY = world.getHighestBlockYAt(bx, bz) + 1; // top of ground = barrel bottom
            Location groundLoc = new Location(world, bx + 0.5, groundY, bz + 0.5);
            landingZoneTask = RenderLandingZoneTask.start(groundLoc, world, plugin);
        }
    }

    public void land(Block block) {
        if (state != State.FALLING) {
            throw new IllegalStateException("Cannot land a crate that is not in FALLING state");
        }

        this.blockChest = block;
        this.landedLocation = block.getLocation().clone();
        this.state = State.LANDED;

        blockChest.setType(Material.BARREL);
        BlockState barrelState = blockChest.getState();
        if (!(barrelState instanceof Barrel barrel)) {
            throw new IllegalStateException("Failed to create barrel at landed location");
        }

        if (displayName != null && !displayName.isEmpty()) {
            barrel.setCustomName(ChatColor.translateAlternateColorCodes('&', displayName));
            barrel.update();
        }

        int overflowCount = 0;
        for (ItemStack is : contents) {
            Map<Integer, ItemStack> overflow = barrel.getInventory().addItem(is);
            for (ItemStack remaining : overflow.values()) {
                if (remaining != null && !remaining.getType().isAir()) {
                    overflowCount++;
                    world.dropItemNaturally(landedLocation.clone().add(0.5, 0.5, 0.5), remaining);
                }
            }
        }
        if (overflowCount > 0) {
            AirdropLogger.warning("Dropped " + overflowCount + " overflow item stack(s) at landed crate");
        }

        CrateManager.addCrate(barrel.getLocation(), this);
        DatabaseManager.save(landedLocation, displayName, trapCrate, teamCrateEnabled, requiredPlayers);
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

        if (ConfigKeys.isHologramEnabled()) {
            hologram = new Hologram(landedLocation, buildHologramLines());
        }

        // Start expiry timer
        expiryTicks = options.getExpiryTicks();
        landTime = System.currentTimeMillis();
        if (expiryTicks > 0 && plugin != null) {
            expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == State.LANDED && !opened && !destroyed) {
                    // EXPIRED: server takes the loot, no drop
                    destroy();
                    AirdropLogger.info("Crate expired and was destroyed (loot taken by server).");
                }
            }, expiryTicks);

            // Update hologram every second to show countdown
            hologramUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (state == State.LANDED && !opened && !destroyed && hologram != null && !hologram.isDead()) {
                    hologram.updateLines(buildHologramLines());
                }
            }, 20L, 20L);
        }

        if (flareEffect != null) {
            flareEffect.cancel();
        }

        // Cancel landing zone marker
        if (landingZoneTask != null) {
            landingZoneTask.cancel();
            landingZoneTask = null;
        }
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
     * CrateManager removal is handled by the caller (CrateCloseListener) after delay.
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

        if (state == State.FALLING && fallingCrate != null && !fallingCrate.isDead()) {
            fallingCrate.setGravity(true);
            fallingCrate.remove();
        }

        if (parachuteSystem != null) parachuteSystem.cancel();

        if (state == State.LANDED) {
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
                world.dropItemNaturally(dropLoc, item);
            }
        }
        barrel.getInventory().clear();
    }

    // ─── TRAP ───────────────────────────────────────────────────────

    public void triggerTrap() {
        if (!trapCrate || landedLocation == null) return;

        List<String> mobs = ConfigKeys.getCrateTrapMobs();
        if (!mobs.isEmpty()) {
            String mobType = mobs.get(new java.util.Random().nextInt(mobs.size()));
            try {
                EntityType entityType = EntityType.valueOf(mobType.toUpperCase());
                for (int i = 0; i < 3; i++) {
                    Location spawnLoc = landedLocation.clone().add(
                            (Math.random() - 0.5) * 3,
                            1,
                            (Math.random() - 0.5) * 3);
                    LivingEntity entity = (LivingEntity) world.spawnEntity(spawnLoc, entityType);
                    entity.setCustomName(ChatColor.translateAlternateColorCodes('&', "&c&lTrap!"));
                    entity.setCustomNameVisible(true);
                }
            } catch (IllegalArgumentException e) {
                AirdropLogger.warning("Invalid trap mob type: " + mobType);
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
                    .replace("{x}", String.valueOf(landedLocation.getBlockX()))
                    .replace("{z}", String.valueOf(landedLocation.getBlockZ()));

            if (expiryTicks > 0) {
                long elapsedMs = System.currentTimeMillis() - landTime;
                long totalMs = (long) expiryTicks * 50;
                long remainingMs = totalMs - elapsedMs;
                if (remainingMs < 0) remainingMs = 0;
                processed = processed.replace("{time}", formatTimeRemaining(remainingMs));
            } else {
                processed = processed.replace("{time}", "");
            }

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

            lines.add(processed);
        }

        return lines;
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
    }

    private void removeHologram() {
        if (hologram != null) {
            hologram.destroy();
            hologram = null;
        }
    }

    // ─── GETTERS / SETTERS ──────────────────────────────────────────

    public State getState() { return state; }
    public FallingBlock getFallingCrate() { return fallingCrate; }
    public Location getLocation() { return state == State.FALLING ? dropLocation : landedLocation; }
    public Location getDropLocation() { return dropLocation; }
    public Location getLandedLocation() { return state == State.LANDED ? landedLocation : null; }
    public boolean getOpened() { return opened; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isTrapCrate() { return trapCrate; }
    public boolean isTeamCrate() { return teamCrateEnabled; }
    public int getRequiredPlayers() { return requiredPlayers; }
    public String getDisplayName() { return displayName; }

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
    public static Crate createPersisted(Location location, String displayName, boolean trapCrate,
                                        boolean isTeamCrate, int requiredPlayers) {
        Crate crate = new Crate(location, location.getWorld(), new ArrayList<>(),
                DropOptions.createDefault(), displayName, trapCrate);
        crate.state = State.LANDED;
        crate.landedLocation = location.clone();
        crate.blockChest = location.getBlock();
        crate.teamCrateEnabled = isTeamCrate;
        crate.requiredPlayers = requiredPlayers;
        return crate;
    }

    private SupplyDrop getEnabledPlugin() {
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        return (plugin != null && plugin.isEnabled()) ? plugin : null;
    }
}
