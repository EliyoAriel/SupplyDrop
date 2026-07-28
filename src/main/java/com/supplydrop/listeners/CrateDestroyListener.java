package com.supplydrop.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.supplydrop.Crate;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.HistoryManager;

public class CrateDestroyListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BARREL) return;
        Location barrelLocation = e.getBlock().getLocation();
        Crate crate = CrateManager.getCrate(barrelLocation);
        if (crate == null) return;

        // Cancel pending cleanup from CrateOpenListener
        CrateOpenListener.cancelCleanup(barrelLocation);

        // LOCK state: use configurable break behavior
        if (crate.isLocked()) {
            HistoryManager.logEvent("DESTROY", crate.getDisplayName(), barrelLocation, null, "lock_break");
            crate.destroyDuringLock();
            CrateManager.removeCrate(barrelLocation);
            return;
        }

        if (crate.getOpened()) {
            // Already opened — just destroy (cleanup handles items)
            CrateManager.removeCrateAndDestroy(barrelLocation);
            return;
        }

        // Not yet opened — determine destruction type based on crate conditions
        if (crate.isTeamCrate()) {
            // Team crate broken manually — punish: destroy items, no drop
            HistoryManager.logEvent("DESTROY", crate.getDisplayName(), barrelLocation, null, "team_manual");
            crate.destroyTeamCrate();
            CrateManager.removeCrate(barrelLocation);
        } else if (crate.isTrapCrate()) {
            // Trap crate broken manually — fire trap, then destroy
            HistoryManager.logEvent("DESTROY", crate.getDisplayName(), barrelLocation, null, "trap_manual");
            crate.destroyWithTrap();
            CrateManager.removeCrate(barrelLocation);
        } else {
            // Normal crate — drop items on ground
            HistoryManager.logEvent("DESTROY", crate.getDisplayName(), barrelLocation, null, "normal");
            crate.destroyAndDropContents();
            CrateManager.removeCrate(barrelLocation);
        }
    }
}
