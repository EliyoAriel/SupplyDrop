package com.supplydrop.listeners;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

import com.supplydrop.helpers.CrateManager;

public class CrateCloseListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getType() != InventoryType.BARREL) return;
        if (!(e.getInventory().getHolder() instanceof Barrel barrel)) return;

        Location barrelLocation = barrel.getBlock().getLocation();
        if (CrateManager.getCrate(barrelLocation) == null) return;

        // If barrel is already empty, destroy immediately (don't wait for 3sec delay)
        if (barrel.getInventory().isEmpty()) {
            barrel.getWorld().playEffect(barrel.getLocation(), Effect.STEP_SOUND, Material.BARREL);
            CrateOpenListener.cancelCleanup(barrelLocation);
            CrateManager.removeCrateAndDestroy(barrelLocation);
        }
    }
}
