package com.supplydrop.integration;

import com.supplydrop.helpers.AirdropLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Reflection hook into LandClaim's static API (no compile dependency).
 * Falls back to permissive behaviour when LandClaim is absent so SupplyDrop
 * works standalone. PDC tags written here are read by LandClaim to exempt
 * crates and parachute entities from claim protection.
 */
public final class LandClaimHook {

    public static final NamespacedKey CRATE_KEY = new NamespacedKey("supplydrop", "crate");
    public static final NamespacedKey PARACHUTE_KEY = new NamespacedKey("supplydrop", "parachute");
    public static final NamespacedKey CRATE_LOOT_KEY = new NamespacedKey("supplydrop", "crate-loot");

    private static boolean available = false;
    private static Method canBuild;
    private static Method isInClaim;
    private static Method registerParachuteSpawn;
    private static Method clearParachuteSpawn;

    private LandClaimHook() {}

    public static void init() {
        available = false;
        canBuild = null;
        isInClaim = null;
        registerParachuteSpawn = null;
        clearParachuteSpawn = null;
        try {
            Plugin landClaim = Bukkit.getPluginManager().getPlugin("LandClaim");
            if (landClaim == null || !landClaim.isEnabled()) return;

            Class<?> apiClass = Class.forName("com.landclaim.api.LandClaimAPI");
            canBuild = apiClass.getMethod("canBuild", Player.class, Location.class);
            isInClaim = apiClass.getMethod("isInClaim", Location.class);
            registerParachuteSpawn = apiClass.getMethod("registerParachuteSpawn", Location.class);
            clearParachuteSpawn = apiClass.getMethod("clearParachuteSpawn", Location.class);
            available = true;
            AirdropLogger.debug("LandClaim integration enabled");
        } catch (Exception e) {
            available = false;
            canBuild = null;
            isInClaim = null;
            registerParachuteSpawn = null;
            clearParachuteSpawn = null;
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * True when the location is inside a claim AND the player (owner/trusted
     * member) can build there. False when LandClaim is absent — used to grant
     * claim-owner bypasses in protection checks without disabling them.
     */
    public static boolean isClaimOwnerBypass(Player player, Location loc) {
        if (!available || player == null || loc == null) return false;
        if (!isInClaim(loc)) return false;
        try {
            return Boolean.TRUE.equals(canBuild.invoke(null, player, loc));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * True when LandClaim is active and the location is inside a claim.
     */
    public static boolean isInClaim(Location loc) {
        if (!available || loc == null) return false;
        try {
            return Boolean.TRUE.equals(isInClaim.invoke(null, loc));
        } catch (Exception e) {
            return false;
        }
    }

    public static void tagCrate(Block block, UUID crateId) {
        if (block == null) return;
        if (block.getState() instanceof TileState tile) {
            tile.getPersistentDataContainer().set(CRATE_KEY, PersistentDataType.STRING, crateId.toString());
            tile.update();
        }
    }

    public static void tagParachute(Entity entity) {
        if (entity == null) return;
        entity.getPersistentDataContainer().set(PARACHUTE_KEY, PersistentDataType.BOOLEAN, true);
    }

    /**
     * Register a location that is about to receive a parachute/hologram spawn,
     * so LandClaim's mobs-flag check can exempt it during the spawn event.
     */
    public static void registerParachuteSpawn(Location loc) {
        if (!available || loc == null) return;
        try {
            registerParachuteSpawn.invoke(null, loc);
        } catch (Exception ignored) {}
    }

    public static void clearParachuteSpawn(Location loc) {
        if (!available || loc == null) return;
        try {
            clearParachuteSpawn.invoke(null, loc);
        } catch (Exception ignored) {}
    }

    public static void tagCrateLoot(org.bukkit.entity.Item item) {
        if (item == null) return;
        item.getPersistentDataContainer().set(CRATE_LOOT_KEY, PersistentDataType.BOOLEAN, true);
    }
}
