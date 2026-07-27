package com.supplydrop.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import com.supplydrop.Crate;
import com.supplydrop.helpers.CrateManager;

public class FallingCrateListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof FallingBlock)) return;

        FallingBlock fallingBlock = (FallingBlock) entity;
        Crate landedCrate = CrateManager.removeCrate(fallingBlock);
        if (landedCrate == null) return;

        e.setCancelled(true);
        fallingBlock.remove();

        Location loc = entity.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            landedCrate.destroy();
            return;
        }

        try {
            landedCrate.land(loc.getBlock());
        } catch (RuntimeException landFailure) {
            Location landedLocation = landedCrate.getLandedLocation();
            if (landedLocation != null) {
                CrateManager.removeCrateAndDestroy(landedLocation);
            } else {
                landedCrate.destroy();
            }
            throw landFailure;
        }
    }
}
