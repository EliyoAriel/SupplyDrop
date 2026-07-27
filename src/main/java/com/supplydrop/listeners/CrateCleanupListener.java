package com.supplydrop.listeners;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import com.supplydrop.Crate;
import com.supplydrop.helpers.CrateManager;

public class CrateCleanupListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        removeCrates(e.blockList(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        removeCrates(e.blockList(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (e.getBlock().getType() == Material.BARREL) {
            Crate crate = CrateManager.getCrate(e.getBlock().getLocation());
            if (crate != null) {
                CrateOpenListener.cancelCleanup(e.getBlock().getLocation());
                // Explosion/burn: always drop items
                crate.destroyAndDropContents();
                CrateManager.removeCrate(e.getBlock().getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent e) {
        Chunk chunk = e.getChunk();
        CrateManager.removeFallingCratesInChunk(chunk);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        CrateManager.removeCratesInWorld(e.getWorld());
    }

    private void removeCrates(List<Block> blocks, boolean dropItems) {
        for (Block block : blocks) {
            if (block.getType() == Material.BARREL) {
                Crate crate = CrateManager.getCrate(block.getLocation());
                if (crate != null) {
                    CrateOpenListener.cancelCleanup(block.getLocation());
                    if (dropItems) {
                        crate.destroyAndDropContents();
                    } else {
                        crate.destroy();
                    }
                    CrateManager.removeCrate(block.getLocation());
                }
            }
        }
    }
}
