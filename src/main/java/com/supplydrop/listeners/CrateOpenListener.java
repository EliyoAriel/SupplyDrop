package com.supplydrop.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitTask;

import com.supplydrop.SupplyDrop;
import com.supplydrop.controllers.DropController;
import com.supplydrop.Crate;
import com.supplydrop.helpers.AirdropLogger;
import com.supplydrop.helpers.ChatHandler;
import com.supplydrop.helpers.CrateManager;
import com.supplydrop.helpers.HistoryManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrateOpenListener implements Listener {

    // Track crates that have been opened and are awaiting cleanup
    private static final Map<Location, BukkitTask> pendingCleanups = new HashMap<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.BARREL) return;
        if (!(e.getInventory().getHolder() instanceof Barrel barrel)) return;
        if (!(e.getPlayer() instanceof Player player)) return;

        Location barrelLocation = barrel.getBlock().getLocation();
        Crate crate = CrateManager.getCrate(barrelLocation);
        if (crate == null) return;
        if (crate.getOpened()) return;

        // LOCK state: completely block opening
        if (crate.isLocked()) {
            e.setCancelled(true);
            long remainingMs = crate.getLockRemainingMs();
            long remainingSec = remainingMs / 1000;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c&lCrate Locked &7- Opens in &e" + remainingSec + "s&7."));
            return;
        }

        // Team crate check
        if (!crate.handleRightClick(player)) {
            e.setCancelled(true);
            return;
        }

        crate.setOpened(true);

        // Reset loot scaling counter
        DropController.markLootCollected();

        // Log event
        HistoryManager.logEvent(crate.isTrapCrate() ? "TRAP" : "OPEN",
                crate.getDisplayName(), barrelLocation, player.getName(), null);

        // Broadcast team crate contributors
        if (crate.isTeamCrate()) {
            List<String> contributors = crate.getContributorNames();
            String joined = String.join("&7, &f", contributors);
            ChatHandler.broadcast("&e&lTeam Crate &7opened by &f" + joined + "&7!");
        }

        // Trigger trap if applicable
        if (crate.isTrapCrate()) {
            crate.triggerTrap();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c&lIt's a trap!"));
        }

        // Schedule delayed cleanup: after 3 seconds, drop remaining items and destroy
        SupplyDrop plugin = SupplyDrop.getPluginInstance();
        if (plugin != null) {
            BukkitTask cleanupTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!crate.isDestroyed()) {
                    crate.dropAllContents();
                    crate.destroy();
                    AirdropLogger.info("Crate cleanup: dropped remaining items and destroyed barrel.");
                }
                pendingCleanups.remove(barrelLocation);
            }, 60L); // 3 seconds = 60 ticks

            pendingCleanups.put(barrelLocation, cleanupTask);
        }
    }

    /**
     * Cancel pending cleanup for a location (e.g., if crate is destroyed by other means).
     */
    public static void cancelCleanup(Location location) {
        BukkitTask task = pendingCleanups.remove(location);
        if (task != null) {
            task.cancel();
        }
    }
}
