package com.supplydrop.controllers;

import com.supplydrop.Crate;
import com.supplydrop.config.ConfigKeys;
import com.supplydrop.config.ConfigKeys.TemplateWeight;
import com.supplydrop.config.DropOptions;
import com.supplydrop.exceptions.SkyNotClearException;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.loot.LootTable;
import com.supplydrop.packages.Package;
import com.supplydrop.packages.PackageManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DropController {

    private static final double HALF_BLOCK = 0.5;
    private static final Random random = new Random();

    // Track time since last successful drop for loot scaling
    private static long lastDropTime = System.currentTimeMillis();
    private static int dropsWithoutLoot = 0;

    /**
     * Call a random drop at a player's location using a weighted template from config.
     */
    public static void callRandomDrop(Player player) throws SkyNotClearException {
        List<TemplateWeight> templates = ConfigKeys.getAutoDropTemplates();
        if (templates.isEmpty()) {
            ChatHandler.sendError(player, "No auto-drop templates configured.");
            return;
        }

        int totalWeight = templates.stream().mapToInt(TemplateWeight::weight).sum();
        if (totalWeight <= 0) {
            ChatHandler.sendError(player, "All template weights are zero.");
            return;
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
            ChatHandler.sendError(player, "Template '" + selectedName + "' not found.");
            return;
        }

        callNamedDrop(pkg, player);
    }

    /**
     * Call a drop using a specific loot table template.
     */
    public static void callNamedDrop(Package pkg, Player player) throws SkyNotClearException {
        DropOptions options = DropOptions.createDefault();
        World world = player.getWorld();
        Location playerLoc = player.getLocation();
        Location spawnLocation = getSpawnLocation(world, playerLoc, options);

        int bonusRolls = calculateBonusRolls();
        List<ItemStack> loot = rollLoot(pkg.getLootTable(), bonusRolls);
        boolean isTrap = rollTrapChance();
        dropCrateAtLocation(spawnLocation, world, loot, options, pkg.getDisplayName(), isTrap);
    }

    /**
     * Drop at a specific location (for auto-drops).
     */
    public static void dropAtLocation(Package pkg, World world, Location loc) throws SkyNotClearException {
        DropOptions options = DropOptions.createDefault();
        Location spawnLocation = getSpawnLocation(world, loc, options);

        int bonusRolls = calculateBonusRolls();
        List<ItemStack> loot = rollLoot(pkg.getLootTable(), bonusRolls);
        boolean isTrap = rollTrapChance();
        dropCrateAtLocation(spawnLocation, world, loot, options, pkg.getDisplayName(), isTrap);
    }

    /**
     * Drop multiple crates at a location (wave drops).
     */
    public static List<Crate> dropWave(Package pkg, World world, Location centerLoc, int count) throws SkyNotClearException {
        List<Crate> crates = new ArrayList<>();
        DropOptions options = DropOptions.createDefault();
        int radius = ConfigKeys.getAutoDropRandomRadius();

        for (int i = 0; i < count; i++) {
            // Scatter crates around the center location
            Location offset = centerLoc.clone().add(
                    (random.nextDouble() - 0.5) * radius * 0.2,
                    0,
                    (random.nextDouble() - 0.5) * radius * 0.2);

            try {
                Location spawnLocation = getSpawnLocation(world, offset, options);
                int bonusRolls = calculateBonusRolls();
                List<ItemStack> loot = rollLoot(pkg.getLootTable(), bonusRolls);
                boolean isTrap = rollTrapChance();
                Crate crate = dropCrateAtLocation(spawnLocation, world, loot, options, pkg.getDisplayName(), isTrap);
                if (crate != null) crates.add(crate);
            } catch (SkyNotClearException e) {
                // Skip this crate if sky not clear
            }
        }
        return crates;
    }

    /**
     * Mark a drop as completed (for loot scaling tracking).
     */
    public static void markDropCompleted() {
        lastDropTime = System.currentTimeMillis();
        dropsWithoutLoot++;
    }

    /**
     * Mark loot as collected (resets escalating counter).
     */
    public static void markLootCollected() {
        dropsWithoutLoot = 0;
    }

    /**
     * Calculate bonus rolls based on loot scaling config.
     */
    private static int calculateBonusRolls() {
        if (!ConfigKeys.isAutoDropLootScaling()) return 0;

        long elapsed = System.currentTimeMillis() - lastDropTime;
        long intervalTicks = ConfigKeys.getAutoDropInterval();
        long intervalMs = intervalTicks * 50; // ticks to ms

        if (intervalMs <= 0) return 0;

        int bonus = (int) (elapsed / intervalMs);
        int maxBonus = ConfigKeys.getAutoDropLootScalingMax();
        return Math.min(bonus, maxBonus);
    }

    /**
     * Roll trap chance.
     */
    private static boolean rollTrapChance() {
        int chance = ConfigKeys.getCrateTrapChance();
        if (chance <= 0) return false;
        return random.nextInt(100) < chance;
    }

    /**
     * Roll items from a loot table based on configured min/max rolls + bonus.
     */
    private static List<ItemStack> rollLoot(LootTable table, int bonusRolls) {
        int min = ConfigKeys.getMinRolls();
        int max = ConfigKeys.getMaxRolls() + bonusRolls;
        return table.rollMultiple(min, max);
    }

    private static Location getSpawnLocation(World world, Location loc, DropOptions options) throws SkyNotClearException {
        Location highestLocation = world.getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getLocation()
                .add(HALF_BLOCK, 0, HALF_BLOCK);

        if (loc.getBlockY() < highestLocation.getBlockY()) {
            throw new SkyNotClearException(loc);
        }

        return highestLocation.add(0, options.getDropHeight(), 0);
    }

    private static Crate dropCrateAtLocation(Location spawnLocation, World world, List<ItemStack> loot, DropOptions options, String displayName, boolean isTrap) {
        Crate crate = new Crate(spawnLocation.clone(), world, loot, options, displayName, isTrap);
        crate.dropCrate();
        return crate;
    }

    /**
     * Bypass spawn: force specific conditions (for admin commands).
     */
    public static void spawnForced(Package pkg, Player player, boolean forceTeam, boolean forceTrap, int teamPlayers, int expiryTicks) throws SkyNotClearException {
        DropOptions options = DropOptions.createDefault().withExpiryTicks(expiryTicks);
        World world = player.getWorld();
        Location spawnLocation = getSpawnLocation(world, player.getLocation(), options);

        List<ItemStack> loot = rollLoot(pkg.getLootTable(), 0);
        Crate crate = new Crate(spawnLocation.clone(), world, loot, options, pkg.getDisplayName(), forceTrap);

        if (forceTeam) {
            crate.forceTeamCrate(teamPlayers);
        }

        crate.dropCrate();
    }

    /**
     * Bypass wave spawn: force multiple crates with conditions.
     */
    public static void spawnWaveForced(Package pkg, Player player, int count, boolean forceTeam, boolean forceTrap, int teamPlayers, int expiryTicks) throws SkyNotClearException {
        DropOptions options = DropOptions.createDefault().withExpiryTicks(expiryTicks);
        World world = player.getWorld();
        Location centerLoc = player.getLocation();

        for (int i = 0; i < count; i++) {
            Location offset = centerLoc.clone().add(
                    (random.nextDouble() - 0.5) * 20,
                    0,
                    (random.nextDouble() - 0.5) * 20);

            try {
                Location spawnLocation = getSpawnLocation(world, offset, options);
                List<ItemStack> loot = rollLoot(pkg.getLootTable(), 0);
                Crate crate = new Crate(spawnLocation.clone(), world, loot, options, pkg.getDisplayName(), forceTrap);

                if (forceTeam) {
                    crate.forceTeamCrate(teamPlayers);
                }

                crate.dropCrate();
            } catch (SkyNotClearException e) {
                // Skip this crate if sky not clear
            }
        }
    }
}
