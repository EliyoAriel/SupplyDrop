package com.supplydrop.helpers;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;

import com.supplydrop.Crate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of all active crates.
 * Uses ConcurrentHashMap for lock-free reads.
 * Write operations use computeIfAbsent/remove for atomicity.
 */
public class CrateManager {

    private CrateManager() {}

    private static final Map<FallingBlock, Crate> crateMap = new ConcurrentHashMap<>();
    private static final Map<BlockKey, Crate> landedCrateMap = new ConcurrentHashMap<>();

    // ─── FALLING CRATES ─────────────────────────────────────────────

    public static void addCrate(FallingBlock block, Crate crate) {
        if (block != null && crate != null) {
            crateMap.put(block, crate);
        }
    }

    public static Crate removeCrate(FallingBlock block) {
        return block != null ? crateMap.remove(block) : null;
    }

    public static Crate getCrate(FallingBlock block) {
        return block != null ? crateMap.get(block) : null;
    }

    // ─── LANDED CRATES ──────────────────────────────────────────────

    public static void addCrate(Location location, Crate crate) {
        BlockKey key = toBlockKey(location);
        if (key != null && crate != null) {
            landedCrateMap.put(key, crate);
        }
    }

    public static Crate removeCrate(Location location) {
        BlockKey key = toBlockKey(location);
        return key != null ? landedCrateMap.remove(key) : null;
    }

    public static Crate getCrate(Location location) {
        BlockKey key = toBlockKey(location);
        return key != null ? landedCrateMap.get(key) : null;
    }

    // ─── COMBINED OPERATIONS ────────────────────────────────────────

    public static boolean removeCrateAndDestroy(Location location) {
        Crate crate = removeCrate(location);
        if (crate == null) return false;
        crate.destroy();
        return true;
    }

    public static boolean removeCrateAndDestroy(FallingBlock block) {
        Crate crate = removeCrate(block);
        if (crate == null) return false;
        crate.destroy();
        return true;
    }

    // ─── BULK OPERATIONS ────────────────────────────────────────────

    public static void removeFallingCratesInChunk(Chunk chunk) {
        if (chunk == null) return;
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        List<FallingBlock> toRemove = new ArrayList<>();

        for (Map.Entry<FallingBlock, Crate> entry : crateMap.entrySet()) {
            FallingBlock fb = entry.getKey();
            if (fb == null || fb.getWorld() == null) {
                toRemove.add(fb);
                continue;
            }
            if (!fb.getWorld().equals(chunk.getWorld())) continue;
            int locChunkX = fb.getLocation().getBlockX() >> 4;
            int locChunkZ = fb.getLocation().getBlockZ() >> 4;
            if (locChunkX == chunkX && locChunkZ == chunkZ) {
                toRemove.add(fb);
            }
        }
        for (FallingBlock fb : toRemove) {
            removeCrateAndDestroy(fb);
        }
    }

    public static void removeCratesInWorld(World world) {
        if (world == null) return;
        UUID worldId = world.getUID();

        List<FallingBlock> fallingToRemove = new ArrayList<>();
        for (Map.Entry<FallingBlock, Crate> entry : crateMap.entrySet()) {
            FallingBlock fb = entry.getKey();
            if (fb == null || fb.getWorld() == null || !worldId.equals(fb.getWorld().getUID())) continue;
            fallingToRemove.add(fb);
        }
        for (FallingBlock fb : fallingToRemove) {
            removeCrateAndDestroy(fb);
        }

        List<Location> landedToRemove = new ArrayList<>();
        for (Map.Entry<BlockKey, Crate> entry : landedCrateMap.entrySet()) {
            BlockKey key = entry.getKey();
            if (key == null || !worldId.equals(key.worldId())) continue;
            landedToRemove.add(new Location(world, key.x(), key.y(), key.z()));
        }
        for (Location loc : landedToRemove) {
            removeCrateAndDestroy(loc);
        }
    }

    public static void clearAll() {
        Set<Crate> crates = new HashSet<>();
        crates.addAll(crateMap.values());
        crates.addAll(landedCrateMap.values());
        for (Crate crate : crates) {
            if (crate != null) crate.destroy();
        }
        crateMap.clear();
        landedCrateMap.clear();
    }

    // ─── QUERIES ────────────────────────────────────────────────────

    public static List<Crate> getActiveCrates() {
        List<Crate> active = new ArrayList<>();
        for (Crate crate : crateMap.values()) {
            if (crate != null && !crate.getOpened()) {
                active.add(crate);
            }
        }
        for (Crate crate : landedCrateMap.values()) {
            if (crate != null && !crate.getOpened()) {
                active.add(crate);
            }
        }
        return active;
    }

    public static int getFallingCrateCount() {
        return crateMap.size();
    }

    public static int getTotalCrateCount() {
        return crateMap.size() + landedCrateMap.size();
    }

    // ─── INTERNAL ───────────────────────────────────────────────────

    private static BlockKey toBlockKey(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return new BlockKey(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {}
}
