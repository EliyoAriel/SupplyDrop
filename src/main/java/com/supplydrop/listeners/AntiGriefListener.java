package com.supplydrop.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

import com.supplydrop.config.ConfigKeys;
import com.supplydrop.helpers.CrateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Protects the area around supply crates from griefing.
 * - Prevents block placement near crates
 * - Prevents pistons from pushing/pulling crate barrels
 */
public class AntiGriefListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        int radius = ConfigKeys.getCrateProtectionRadius();
        if (radius <= 0) return;

        Block placed = e.getBlock();
        if (!isNearCrate(placed.getLocation(), radius)) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&cYou cannot build near a supply crate!"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        // Don't handle crate barrel breaks — that's CrateDestroyListener
        if (e.getBlock().getType() == Material.BARREL) return;

        int radius = ConfigKeys.getCrateProtectionRadius();
        if (radius <= 0) return;

        Block broken = e.getBlock();
        if (!isNearCrate(broken.getLocation(), radius)) return;

        e.setCancelled(true);
        e.getPlayer().sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&cYou cannot break blocks near a supply crate!"));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        int radius = ConfigKeys.getCrateProtectionRadius();
        if (radius <= 0) return;

        // Check if any block in the piston push would affect a crate
        for (Block block : e.getBlocks()) {
            if (CrateManager.getCrate(block.getLocation()) != null) {
                e.setCancelled(true);
                return;
            }
        }

        // Check if the piston is pushing blocks near a crate
        List<Block> affected = new ArrayList<>(e.getBlocks());
        affected.add(e.getBlock().getRelative(e.getDirection()));
        for (Block block : affected) {
            if (isNearCrate(block.getLocation(), radius) && !isCrateBlock(block)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        int radius = ConfigKeys.getCrateProtectionRadius();
        if (radius <= 0) return;

        if (e.isSticky()) {
            Block pulled = e.getBlock().getRelative(e.getDirection());
            if (CrateManager.getCrate(pulled.getLocation()) != null) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private boolean isNearCrate(Location loc, int radius) {
        // Check all active crates
        for (com.supplydrop.Crate crate : com.supplydrop.helpers.CrateManager.getActiveCrates()) {
            if (crate.getLandedLocation() == null) continue;
            if (!crate.getLandedLocation().getWorld().equals(loc.getWorld())) continue;
            double distance = crate.getLandedLocation().distance(loc);
            if (distance <= radius) return true;
        }
        return false;
    }

    private boolean isCrateBlock(Block block) {
        return CrateManager.getCrate(block.getLocation()) != null;
    }
}
